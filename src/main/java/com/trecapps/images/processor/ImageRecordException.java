package com.trecapps.images.processor;

import com.trecapps.images.models.ImageRecord;
import lombok.Getter;

public class ImageRecordException extends RuntimeException {
    @Getter
    ImageRecord imageRecord;
    public ImageRecordException(String message, ImageRecord record) {
        super(message);
        imageRecord = record;
    }
}
