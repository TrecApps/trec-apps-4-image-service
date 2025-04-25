package com.trecapps.images.models;

import lombok.Getter;

public enum ImageState {
    UPLOADED(false), // Just uploaded, unsure what the status is, do nothing once analysis is performed
    ERROR(false), // Error in processing the image
    ADULT(false), // Image is considered adult material, DO NOT make public
    NON_ADULT(false), // Image is not adult, but not public either
    PRE_PUBLIC(true), // similar to UPLOADED, but make public once analysis is done (if not adult)
    PRE_PROFILE(true), // siilar to PRE_PUBLIC, but make the image the profile of the uploader
    PUBLIC(true) // Image is in the public repository, no access restrictions are in place
    ;

    ImageState(boolean usePublic){
        this.usePublic = usePublic;
    }
    @Getter
    final boolean usePublic;
}
