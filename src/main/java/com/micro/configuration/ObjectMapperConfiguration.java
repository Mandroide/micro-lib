package com.micro.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Slf4j
@Configuration
public class ObjectMapperConfiguration {

    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder().featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        log.info("Configuring JSON ObjectMapper");
        return jackson2ObjectMapperBuilder().createXmlMapper(false).build();
    }

    @Bean
    public XmlMapper xmlMapper() {
        log.info("Configuring XML ObjectMapper :: XMLMapper");
        return jackson2ObjectMapperBuilder().createXmlMapper(true).build();
    }
}
