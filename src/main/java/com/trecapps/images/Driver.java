package com.trecapps.images;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;



@SpringBootApplication
@ComponentScan({
        "com.trecapps.auth.common.*",
        "com.trecapps.auth.webflux.*",
        "com.trecapps.images.*"})
@EnableWebFlux
@Configuration
public class Driver {
    public static void main(String[] args) {

        SpringApplication.run(Driver.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Enable timestamps
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        return mapper;
    }

}
