package com.micro.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import java.net.SocketTimeoutException;

/**
 * This configuration allows Kafka to retry transactions. By having it, we should always have a configured TID to
 * acknowledge it was already processed.
 */
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
@Configuration
public class KafkaConfiguration {
    private final ObjectMapper objectMapper;
    @Value("${kafka.retry.interval}")
    private long retryInterval;
    @Value("${kafka.retry.max-attempts}")
    private long retryMaxAttempts;

    @Bean
    public DefaultErrorHandler errorHandler() {
        log.info("Configure Kafka retry: {} interval -- {} Max Attempts", retryInterval, retryMaxAttempts);
        BackOff fixedBackOff = new FixedBackOff(retryInterval, retryMaxAttempts);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(((consumerRecord, e) -> {
            // TODO After retries are finished, this should be put in a place or notify
            String json;
            try {
                json = objectMapper.writeValueAsString(consumerRecord.value());
            } catch (JsonProcessingException ex) {
                json = consumerRecord.value().toString();
            }
            log.error("Kafka Record retry Error (Can't process): key: {} -- value: {}", consumerRecord.key(), json, e);
        }), fixedBackOff);

        errorHandler.addRetryableExceptions(SocketTimeoutException.class);
        errorHandler.addNotRetryableExceptions(NullPointerException.class);
        return errorHandler;
    }
}
