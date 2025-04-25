package com.trecapps.images.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class ImageStorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "picture.storage", name = "strategy", havingValue = "azure-blob-key")
    IImageStorageService getAzureStorageServiceByKey(
            @Value("${picture.storage.account-name}") String name,
            @Value("${picture.storage.account-key}") String key,
            @Value("${picture.storage.blob-endpoint}") String endpoint,
            @Value("${picture.storage.public-container}") String publicContainerName,
            @Value("${picture.storage.hidden-container}") String hiddenContainerName,
            Jackson2ObjectMapperBuilder objectMapperBuilder
    ) {
        return new AzureImageStorageService(name, key, endpoint, publicContainerName, hiddenContainerName, objectMapperBuilder);
    }


    @Bean
    @ConditionalOnProperty(prefix = "picture.storage", name = "strategy", havingValue = "azure-blob-default")
    IImageStorageService getAzureStorageServiceByDefault(
            @Value("${picture.storage.blob-endpoint}") String endpoint,
            @Value("${picture.storage.public-container}") String publicContainerName,
            @Value("${picture.storage.hidden-container}") String hiddenContainerName,
            Jackson2ObjectMapperBuilder objectMapperBuilder
    ) {
        return new AzureImageStorageService(objectMapperBuilder, publicContainerName, hiddenContainerName, endpoint);
    }


}
