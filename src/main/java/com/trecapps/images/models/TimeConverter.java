package com.trecapps.images.models;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;



@ReadingConverter
public class TimeConverter implements Converter<String, OffsetDateTime> {
    @Override
    public OffsetDateTime convert(String source) {
        OffsetDateTime ret = OffsetDateTime.parse(source);
        return ret;
    }
}