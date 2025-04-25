package com.trecapps.images.controllers;

import com.azure.core.annotation.Get;
import com.trecapps.auth.common.models.TrecAuthentication;
import com.trecapps.images.models.ImageRecord;
import com.trecapps.images.models.ReaderAction;
import com.trecapps.images.models.ResponseObj;
import com.trecapps.images.services.ImageWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/Image-API")
public class ImageApiController {

    @Autowired
    ImageWriteService imageWriteService;

    @PostMapping
    Mono<ResponseEntity<ResponseObj>> postImage(
            Authentication authentication,
            @RequestHeader("Content-Type") String type,
            @RequestBody byte[] data,
            @RequestParam(required = false) String crop,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String album,
            @RequestParam(required = false) String app,
            @RequestParam(required = false) String name
    ) {
        TrecAuthentication trecAuthentication = (TrecAuthentication) authentication;
        return imageWriteService.postImage(
                trecAuthentication.getUser(),
                trecAuthentication.getBrand(),
                data,
                type,
                crop,
                mode,
                album,
                app,
                name
                ).map(ResponseObj::toEntity);


    }

    @PatchMapping("/readers")
    Mono<ResponseEntity<ResponseObj>> patchReaders(
            Authentication authentication,
            @RequestParam String id,
            @RequestParam(defaultValue = "false") boolean setPrivate,
            @RequestBody ReaderAction action
    ){
        TrecAuthentication trecAuthentication = (TrecAuthentication) authentication;
        return imageWriteService.patchReaders(
                trecAuthentication.getUser(),
                trecAuthentication.getBrand(),
                id,
                setPrivate,
                action
        ).map(ResponseObj::toEntity);
    }

    @PutMapping("/{id}")
    Mono<ResponseEntity<ResponseObj>> setProfile(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam(defaultValue = "main") String app
    ) {
        TrecAuthentication trecAuthentication = (TrecAuthentication) authentication;
        return imageWriteService.setProfilePic(
                trecAuthentication.getUser(),
                trecAuthentication.getBrand(),
                id,
                app
        ).map(ResponseObj::toEntity);
    }

    @GetMapping
    Mono<List<ImageRecord>> getImages(
            Authentication authentication,
            @RequestParam(required = false) String app,
            @RequestParam(required = false) String album,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        TrecAuthentication trecAuthentication = (TrecAuthentication) authentication;
        return imageWriteService.getImageList(
                trecAuthentication.getUser(),
                trecAuthentication.getBrand(),
                app, album, page, size
        );
    }

    @GetMapping("/data/{id}")
    Mono<ResponseEntity<ResponseObj>> getImageAsBase64(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam(required = false) String crop,
            @RequestParam(defaultValue = "false") boolean allowAdult
    ) {
        TrecAuthentication trecAuthentication = (TrecAuthentication) authentication;
        return imageWriteService.getImageAsBase64(
                trecAuthentication.getUser(),
                trecAuthentication.getBrand(),
                id, crop, allowAdult
        ).map(ResponseObj::toEntity);
    }


}
