package com.trecapps.images;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@ComponentScan({
        "com.trecapps.auth.common.*",
        "com.trecapps.auth.webflux.*",
        "com.trecapps.images.*"})
@EnableWebFlux
public class Driver {
    public static void main(String[] args) {
        SpringApplication.run(Driver.class, args);
    }
}
