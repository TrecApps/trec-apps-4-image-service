package com.trecapps.images.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Analysis {

    @JsonProperty("isAdultContent")
    boolean isAdultContent;
    @JsonProperty("isRacyContent")
    boolean isRacyContent;
    @JsonProperty("isGoryContent")
    boolean isGoryContent;

    double adultScore;
    double racyScore;
    double goreScore;

}
