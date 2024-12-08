package com.micro.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
@Configuration
public class KafkaGeneralConfiguration {
    private final DefaultErrorHandler errorHandler;
    private final ObjectMapper objectMapper;
    @Value("${kafka.trusted-packages}")
    private String trustedPackages;
    @Value("${kafka.bootstrap-server}")
    private String bootstrapServer;
    @Value("${kafka.timeout}")
    private long timeout;
    @Value("${kafka.debug.credential:false}")
    private boolean debugCredential;
    @Value("${kafka.aws.enabled}")
    private boolean awsEnabled;
    @Value("${kafka.message.bytes-size}")
    private int messageBytesSize;
    @Value("${kafka.message.buffer-memory}")
    private int messageBufferMemory;
    @Getter
    @Value("${kafka.topic.partitions}")
    private int partitions;
    @Value("${kafka.listener.concurrency}")
    private int listenerConcurrency;
    @Value("${kafka.reconnect.ms.attempt}")
    private int reconnectAttempts;
    @Value("${kafka.reconnect.ms.max}")
    private int reconnectMax;

    @Bean
    public <T> JsonSerializer<T> jsonSerializer() {
        return new JsonSerializer<>(objectMapper);
    }

    @Bean
    public <T> JsonDeserializer<T> jsonDeserializer() {
        return new JsonDeserializer<>(objectMapper);
    }

    /**
     * Enable feature to create topics in runtime
     * Returns: Kafka Admin Bean
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        configs.put(AdminClientConfig.RECONNECT_BACKOFF_MS_CONFIG, reconnectAttempts);
        configs.put(AdminClientConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, reconnectMax);
        addSSLConfiguration(configs);
        return new KafkaAdmin(configs);
    }

    public <T> ProducerFactory<String, T> producerFactoryPrepared() {
        return new DefaultKafkaProducerFactory<>(producerConfig(), new StringSerializer(), jsonSerializer());
    }

    public <T> ConsumerFactory<String, T> consumerFactoryPrepared(String groupId, @NonNull Class<T> defaultType) {
        return new DefaultKafkaConsumerFactory<>(consumerConfig(groupId, defaultType), new StringDeserializer(), jsonDeserializer());
    }

    /**
     * Default configuration builder
     * @return Default Consumer configuration
     */
    public <T> Map<String, Object> consumerConfig(String groupId, @NonNull Class<T> defaultType) {
        log.info("Configure partition fetch size: {}", messageBytesSize);
        return consumerJSONConfig(groupId, defaultType);
    }

    /**
     * Default configuration builder
     * @return Default Producer configuration
     */
    public Map<String, Object> producerConfig() {
        log.info("Configure Request Size: {}", messageBytesSize);
        return producerJSONConfig();
    }

    public <T> KafkaTemplate<String, T> kafkaTemplate(ProducerFactory<String, T> producerFactory, String defaultTopic) {
        KafkaTemplate<String, T> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        kafkaTemplate.setDefaultTopic(defaultTopic);
        return kafkaTemplate;
    }

    public <T, R> ReplyingKafkaTemplate<String, T, R> replyingKafkaTemplate(ProducerFactory<String, T> producer,
                                                                            ConcurrentMessageListenerContainer<String, R> concurrentMessageListenerContainer) {
        ReplyingKafkaTemplate<String, T, R> replyingKafkaTemplate = new ReplyingKafkaTemplate<>(producer, concurrentMessageListenerContainer);
        replyingKafkaTemplate.setDefaultReplyTimeout(Duration.ofMillis(timeout));
        replyingKafkaTemplate.setSharedReplyTopic(true);
        return replyingKafkaTemplate;
    }

    public <T> ConcurrentMessageListenerContainer<String, T> replyContainer(String group, String topicResponse, ConcurrentKafkaListenerContainerFactory<String, T> containerFactory) {
        ConcurrentMessageListenerContainer<String, T> repliesContainer = containerFactory.createContainer(topicResponse);
        repliesContainer.getContainerProperties().setMissingTopicsFatal(true);
        repliesContainer.getContainerProperties().setGroupId(group);
        repliesContainer.setAutoStartup(true);
        return repliesContainer;
    }

    /**
     * Exclusive for reply configuration. It should not be used for configurations only listening requests.
     *
     * @param group           GroupId
     * @param consumerFactory - Consumer Factory
     * @param <T>             - Data type of class parsing consumer
     * @return ConcurrentKafkaListenerContainerFactory
     */
    public <T> ConcurrentKafkaListenerContainerFactory<String, T> concurrentKafkaListenerContainerFactory(String group, ConsumerFactory<String, T> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.getContainerProperties().setMissingTopicsFatal(true);
        factory.getContainerProperties().setGroupId(group);
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(listenerConcurrency);
        factory.setMissingTopicsFatal(true);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    public <T, R> ConcurrentKafkaListenerContainerFactory<String, T> concurrentKafkaListenerContainerFactory(String group, ConsumerFactory<String, T> consumerFactory,
                                                                                                             KafkaTemplate<String, R> replyKafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.getContainerProperties().setMissingTopicsFatal(true);
        factory.getContainerProperties().setGroupId(group);
        factory.setConsumerFactory(consumerFactory);
        factory.setMissingTopicsFatal(true);
        factory.setReplyTemplate(replyKafkaTemplate);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    /**
     * JSON Consumer Configuration
     *
     * @param groupId     - Kafka Group Name
     * @param defaultType - Default class type
     * @param <T>         Class type
     * @return Consumer Configuration Map
     * @see <a href="https://quix.io/blog/kafka-auto-offset-reset-use-cases-and-pitfalls">OFFSET_RESET_CONFIG</a>
     */
    public <T> Map<String, Object> consumerJSONConfig(String groupId, @NonNull Class<T> defaultType) {
        log.info("Configure JSON Consumer");
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.LATEST.toString());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, defaultType.getCanonicalName());
        config.put(JsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
        config.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, messageBytesSize);
        config.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, reconnectAttempts);
        config.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, reconnectMax);
        addSSLConfiguration(config);
        return config;
    }

    public Map<String, Object> producerJSONConfig() {
        log.info("Configure JSON Producer");
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 0);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, messageBufferMemory);
        config.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, messageBytesSize);
        config.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, reconnectAttempts);
        config.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, reconnectMax);
        addSSLConfiguration(config);
        return config;
    }

    public Map<String, String> topicCreationConfiguration() {
        Map<String, String> config = new HashMap<>();
        config.put(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, messageBytesSize + "");
        return config;
    }

    /**
     * To use this configuration, we should have a file called "config" in path
     *
     * <pre>
     * mkdir ~/.aws
     * cd ~/.aws
     * touch config
     * </pre>
     * <p>
     * File sample content (it should be adjusted depending on needs)
     *
     * <pre>
     * [msk_client]
     * role_arn = arn:aws:iam::123456789012:role/msk_client_rolee
     * credential_role = Ec2InstanceMetadata
     * </pre>
     *
     * @param config To modify values in use
     * @see <a href="https://github.com/aws/aws-msk-iam-auth">aws-msk-iam-auth</a>
     */
    private void addSSLConfiguration(Map<String, Object> config) {
        if (awsEnabled) {
            log.info("AWS KAFKA Enabled");
            config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
            config.put(SaslConfigs.SASL_MECHANISM, "AWS_MSK_IAM");
            config.put(SaslConfigs.SASL_JAAS_CONFIG, "software.amazon.msk.auth.iam.IAMLoginModule required awsProfileName=\"ec2-fulfillment-app-role\""
                    + (debugCredential ? "awsDebugCreds=true;" : ";"));
            config.put(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS, "software.amazon.msk.auth.iam.ClientCallbackHandler");
            log.info("Configuration: {} -- {}", SaslConfigs.SASL_JAAS_CONFIG, config.get(SaslConfigs.SASL_JAAS_CONFIG));
        } else {
            log.info("AWS KAFKA Disabled");
        }
    }
}
