package com.trecapps.images.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.trecapps.images.models.Analysis;
import lombok.Data;

@Data
public class ImageMessageModel {

    String imageId;
    Analysis results;

}
