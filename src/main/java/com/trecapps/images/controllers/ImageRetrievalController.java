package com.trecapps.images.controllers;

import com.trecapps.images.models.ImageProfileEntry;
import com.trecapps.images.models.ResponseObj;
import com.trecapps.images.services.GetPublicImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/Images")
@ConditionalOnProperty(prefix = "trecapps.image", value = "public-get", havingValue = "true")
public class ImageRetrievalController {

    @Autowired
    GetPublicImageService imageService;

    @GetMapping("/public/{id}")
    Mono<ResponseEntity<byte[]>> getImage(
            @PathVariable String id, @RequestParam(required = false) String crop
    ) {
        return imageService.getImageById(id, crop);
    }

    @PostMapping("/profile/{profile}")
    Mono<ResponseEntity<ResponseObj>> addProfile(
            @PathVariable String profile,
            @RequestBody List<ImageProfileEntry> entries){
        return imageService.setProfile(profile, entries);
    }

    @GetMapping("/profile/{profile}")
    Mono<ResponseEntity<byte[]>> getProfile(
            @PathVariable String profile, @RequestParam(defaultValue = "main") String app
    ) {
        return imageService.getImageByProfileId(profile, app);
    }

}
