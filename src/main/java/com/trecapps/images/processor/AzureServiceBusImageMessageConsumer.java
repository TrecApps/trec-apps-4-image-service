package com.trecapps.images.processor;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class AzureServiceBusImageMessageConsumer implements IMessageConsumer {

    ServiceBusProcessorClient processorClient;
    IImageMessageHandler messageHandler;

    AzureServiceBusImageMessageConsumer(
            String queue,
            String connector,
            boolean useConnectionString
    ){
        if(useConnectionString){
            processorClient = new ServiceBusClientBuilder()
                    .connectionString(connector)
                    .processor()
                    .queueName(queue)
                    .processMessage(this::processMessage)
                    .processError(this::processError)
                    .buildProcessorClient();
        } else {
            DefaultAzureCredential credential = new DefaultAzureCredentialBuilder()
                    .build();

            processorClient = new ServiceBusClientBuilder()
                    .fullyQualifiedNamespace(String.format("%s.servicebus.windows.net", connector))
                    .credential(credential)
                    .processor()
                    .queueName(queue)
                    .processMessage(this::processMessage)
                    .processError(this::processError)
                    .buildProcessorClient();
        }
    }

    void processMessage(ServiceBusReceivedMessageContext context) {
        Mono<Boolean> processor = Mono.just(context)
                .map(ServiceBusReceivedMessageContext::getMessage)
                .map((ServiceBusReceivedMessage message) -> message.getBody().toObject(ImageMessageModel.class))
                .flatMap(messageHandler::processImageMessage)
                .doOnNext((Boolean worked) -> {
                    if(worked) context.complete();
                    else context.deadLetter();
                })
                .onErrorComplete((Throwable thrown) -> {
                    log.error("Error processing message!", thrown);
                    context.deadLetter();
                    return false;
                });
        processor.subscribe();
    }

    void processError(ServiceBusErrorContext context){

    }

    @Override
    public void initialize(IImageMessageHandler handler) {
        this.messageHandler = handler;
        if(handler != null)
        {
            this.processorClient.start();
        }
    }


}
