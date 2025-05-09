package com.trecapps.images.models;

import org.springframework.core.convert.converter.Converter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeToStringConverter implements Converter<OffsetDateTime, String> {
    @Override
    public String convert(OffsetDateTime source) {
        return source.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
