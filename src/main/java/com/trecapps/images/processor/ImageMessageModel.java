package com.trecapps.images.processor;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class ImageMessageModel {

    String imageId;
    JsonNode results;

}
