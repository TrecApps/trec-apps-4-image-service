package com.trecapps.images.models;

public enum ImageVisibility {
    PUBLIC,         // Accessible without Restriction
    PUBLIC_AUTH,    // Accessible to all authenticated users (restrictions for adult images still apply)
    PROTECTED       // Accessible to the owner and authorized readers
}
