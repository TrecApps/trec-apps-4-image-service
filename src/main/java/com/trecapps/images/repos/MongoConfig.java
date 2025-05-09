package com.trecapps.images.repos;

import com.trecapps.images.models.OffsetDateTimeToStringConverter;
import com.trecapps.images.models.StringToOffsetDateTimeConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import java.util.Arrays;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(Arrays.asList(
                new StringToOffsetDateTimeConverter(),
                new OffsetDateTimeToStringConverter()
        ));
    }
}
