package com.trecapps.images.repos;

import com.trecapps.images.models.TimeConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Arrays;
import java.util.List;

@Configuration
public class MongoConverters {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {

        return new MongoCustomConversions(
                List.of(
                        new TimeConverter()));
    }
}