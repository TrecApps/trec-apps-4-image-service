package com.trecapps.images.models;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document("ImageProfile")
@Data
public class ImageProfile {

    @Id
    String profileId;

    List<ImageProfileEntry> entries = new ArrayList<>();

}
