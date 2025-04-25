package com.trecapps.images.processor;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface IImageMessageHandler {
    Mono<Boolean> processImageMessage(ImageMessageModel message);
}
