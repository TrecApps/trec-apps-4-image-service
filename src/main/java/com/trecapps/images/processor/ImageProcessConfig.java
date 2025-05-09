package com.trecapps.images.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.trecapps.images.models.ImageRecord;
import com.trecapps.images.models.ImageState;
import com.trecapps.images.models.ResponseObj;
import com.trecapps.images.repos.ImageRepo;
import com.trecapps.images.repos.ProfileRepo;
import com.trecapps.images.services.HelperMethods;
import com.trecapps.images.services.IImageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
public class ImageProcessConfig {

    ImageRepo imageRepo;
    IImageStorageService imageStorageService;
    ProfileRepo profileRepo;

    @Autowired
    ImageProcessConfig(
            ImageRepo imageRepo1,
            IImageStorageService imageStorageService1,
            ProfileRepo profileRepo1){
        this.imageRepo = imageRepo1;
        this.imageStorageService = imageStorageService1;
        this.profileRepo = profileRepo1;


        handler = (ImageMessageModel model) -> {
            return this.imageRepo.findById((model.getImageId()))
                    .doOnNext((ImageRecord record) -> {
                        if(record == null) throw new RuntimeException(String.format("Image %s not found in processing!", model.getImageId()));
                        record.setAnalysis(model.getResults());
                    })
                    .flatMap((ImageRecord record) -> {
                        ImageState currentState = record.getState();

                        switch(currentState){
                            case UPLOADED:
                                // Just mark whether it is adult or not, the user didn't wish for it to be public
                                record.setState(record.isPublicEligible() ? ImageState.NON_ADULT : ImageState.ADULT);
                                break;
                            case PRE_PROFILE:
                            case PRE_PUBLIC:
                                // The user wished for it to be public
                                if(record.isPublicEligible()) {
                                    record.setState(ImageState.PUBLIC);
                                    return imageStorageService.transferImage(record)
                                            .map((ResponseObj obj) -> {
                                                if(obj.getHttpStatus().is2xxSuccessful())
                                                    return record;
                                                throw new ImageRecordException(String.format("Error transferring Image %s to public storage!", model.getImageId()), record);
                                            })
                                            .flatMap((ImageRecord imageRecord) -> {
                                                if(ImageState.PRE_PUBLIC.equals(currentState))
                                                    return Mono.just(imageRecord);

                                                return HelperMethods.setProfileImage(profileRepo, imageRecord, imageRecord.getOwner(), imageRecord.getApp())
                                                        .map((ResponseObj obj) -> {
                                                            if(obj.getHttpStatus().is2xxSuccessful())
                                                                return record;
                                                            throw new ImageRecordException(String.format("Error setting image %s to profile pic!", model.getImageId()), record);
                                                        });
                                            });
                                }
                                record.setState(ImageState.ADULT);
                                record.setAllowPublic(true);
                                break;
                            default:
                                throw new ImageRecordException(String.format("Unrecognized image state detected in %s", model.getImageId()), record);
                        }

                        return Mono.just(record);
                    })
                    .flatMap((ImageRecord record) -> imageRepo.save(record))
                    .doOnNext((ImageRecord record) -> log.info("Updated Record {} for user {}. Current Status is {}", record.getId(), record.getOwner(), record.getState()))
                    .onErrorResume(ImageRecordException.class, (ImageRecordException ex) -> {
                        ImageRecord record = ex.getImageRecord();
                        log.error("Error processing image {}:", record.getId(), ex);
                        record.setState(ImageState.ERROR);
                        return imageRepo.save(record);
                    })
                    .map((ImageRecord record) -> !ImageState.ERROR.equals(record.getState()))
                    .onErrorResume((throwable -> {
                        log.error("Error detected in processing image: ", throwable);
                        return Mono.just(false);
                    }));

        };

    }

    IImageMessageHandler handler;








    @Bean
    @ConditionalOnProperty(prefix = "trecapps.imconsumer", name = "strategy", havingValue = "azure-service-bus-entra")
    IMessageConsumer getServiceBusEntra(
            @Value("${trecapps.imconsumer.queue}") String queue,
            @Value("${trecapps.imconsumer.namespace}") String namespace,
            Jackson2ObjectMapperBuilder objectMapperBuilder
    ) {
        return generateConsumer(queue, namespace, objectMapperBuilder, false);
    }


    @Bean
    @ConditionalOnProperty(prefix = "trecapps.imconsumer", name = "strategy", havingValue = "azure-service-bus-connection-string")
    IMessageConsumer getServiceBusConnString(
            @Value("${trecapps.imconsumer.queue}") String queue,
            @Value("${trecapps.imconsumer.connection}") String connection,
            Jackson2ObjectMapperBuilder objectMapperBuilder
    ) {
        return generateConsumer(queue, connection, objectMapperBuilder, true);
    }

    IMessageConsumer generateConsumer(String queue, String nameConn, Jackson2ObjectMapperBuilder objectMapperBuilder, boolean conn){
        IMessageConsumer consumer = new AzureServiceBusImageMessageConsumer(queue, nameConn, objectMapperBuilder, conn);
        consumer.initialize(handler);
        return consumer;
    }

}
