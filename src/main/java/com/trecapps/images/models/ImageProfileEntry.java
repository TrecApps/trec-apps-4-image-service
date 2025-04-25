package com.trecapps.images.models;

import lombok.Data;

@Data
public class ImageProfileEntry {

    String imageId;
    String app;
    String type; // Provided for easier processing of Profile Retrieval

}
