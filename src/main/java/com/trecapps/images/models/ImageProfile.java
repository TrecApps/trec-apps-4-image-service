package com.trecapps.images.models;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.ArrayList;
import java.util.List;

@Document("ImageProfile")
@Data
public class ImageProfile {

    @MongoId
    String profileId;

    List<ImageProfileEntry> entries = new ArrayList<>();

}
