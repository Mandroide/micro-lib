package com.micro.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "pattern", name = "cqrs.enabled", havingValue = "true")
public class CQRSConfiguration {
    @Bean
    public MessagingTemplate messagingTemplate() {
        log.info("Configure CQRS message template");
        return new MessagingTemplate();
    }

    @Bean
    public MessageChannel commandChannel() {
        log.info("Configure CQRS Command channel");
        return new PublishSubscribeChannel();
    }
}
