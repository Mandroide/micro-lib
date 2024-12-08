package com.micro.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.sftp.outbound.SftpMessageHandler;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
@EnableIntegration
@IntegrationComponentScan
@Slf4j
@ConditionalOnProperty(prefix = "sftp", name = "enabled", havingValue = "true")
public class SecureFTPConfiguration {
    @Value("${sftp.host}")
    private String sftpHost;
    @Value("${sftp.port}")
    private int sftpPort;
    @Value("${sftp.user}")
    private String sftpUser;
    @Value("${sftp.password}")
    private String sftpPassword;

    @Bean
    public DefaultSftpSessionFactory sftpSessionFactory() {
        DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory(true);
        sftpSessionFactory.setHost(sftpHost);
        sftpSessionFactory.setPort(sftpPort);
        sftpSessionFactory.setUser(sftpUser);
        sftpSessionFactory.setPassword(sftpPassword);
        sftpSessionFactory.setAllowUnknownKeys(true);
        return sftpSessionFactory;
    }

    @Bean
    public MessageChannel sftpChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "sftpChannel")
    public MessageHandler sftpMessageHandler() {
        SftpMessageHandler sftpMessageHandler = new SftpMessageHandler(sftpSessionFactory());
        sftpMessageHandler.setAutoCreateDirectory(true);
        sftpMessageHandler.setRemoteDirectoryExpressionString("headers".concat(FileHeaders.REMOTE_DIRECTORY));
        sftpMessageHandler.setFileNameGenerator(message -> (String) message.getHeaders().get(FileHeaders.FILENAME));
        return sftpMessageHandler;
    }

}
