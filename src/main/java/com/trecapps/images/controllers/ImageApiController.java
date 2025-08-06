package com.trecapps.images.controllers;

import com.azure.core.annotation.Get;
import com.nimbusds.oauth2.sdk.Response;
import com.trecapps.auth.common.models.TcBrands;
import com.trecapps.auth.common.models.TrecAuthentication;
import com.trecapps.auth.webflux.services.IUserStorageServiceAsync;
import com.trecapps.images.models.*;
import com.trecapps.images.services.ImageWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.ObjectStreamException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/Image-API")
public class ImageApiController {

    @Autowired
    ImageWriteService imageWriteService;

    @Autowired
    IUserStorageServiceAsync userStorageService;

    @PostMapping
    Mono<ResponseEntity<ResponseObj>> postImage(
            Authentication authentication,
            @RequestHeader("Content-Type") String type,
            @RequestBody String data,
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
                Base64.getDecoder().decode(data),
                type,
                crop,
                mode,
                album,
                app,
                name
                ).map(ResponseObj::toEntity);


    }

    @PatchMapping
    Mono<ResponseEntity<ResponseObj>> patchData(
            Authentication authentication,
            @RequestBody ImagePatch patch,
            @RequestParam String id
    ){
        TrecAuthentication trecAuthentication = (TrecAuthentication) authentication;
        return imageWriteService.patchData(
                trecAuthentication.getUser(),
                trecAuthentication.getBrand(),
                id,
                patch
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
            @RequestParam(defaultValue = "main") String app,
            @RequestParam(defaultValue = "") String brand
    ) {
        TrecAuthentication trecAuthentication = (TrecAuthentication) authentication;

        if(brand.isEmpty())
            return imageWriteService.setProfilePic(
                trecAuthentication.getUser(),
                trecAuthentication.getBrand(),
                id,
                app
        ).map(ResponseObj::toEntity);

        return userStorageService.getBrandById(brand)
                .map((Optional<TcBrands> oBrand) -> {
                    if(oBrand.isEmpty())
                        throw new ObjectResponseException("Brand Id Not Found",HttpStatus.NOT_FOUND );

                    TcBrands brandObj = oBrand.get();

                    if(!brandObj.getOwners().contains(trecAuthentication.getUser().getId()))
                        throw new ObjectResponseException("Brand does not belong to you", HttpStatus.FORBIDDEN);
                    trecAuthentication.setBrand(brandObj);
                    return trecAuthentication;
                })
                .flatMap((TrecAuthentication tauth) -> {
                    return imageWriteService.setProfilePic(
                            tauth.getUser(),
                            tauth.getBrand(),
                            id,
                            app
                    );
                })
                .onErrorResume((ObjectResponseException.class), (ObjectResponseException e) -> Mono.just(e.toResponseObj()))
                .map(ResponseObj::toEntity);

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

    @DeleteMapping("/{id}")
    Mono<ResponseEntity<ResponseObj>> deleteImage(
            Authentication authentication,
            @PathVariable String id
    ) {
        TrecAuthentication trecAuthentication = (TrecAuthentication) authentication;
        return imageWriteService.startDelete(
                trecAuthentication.getUser(),
                trecAuthentication.getBrand(),
                id
        ).map(ResponseObj::toEntity);
    }

    @DeleteMapping("/cancel/{id}")
    Mono<ResponseEntity<ResponseObj>> cancelDeletion(
            Authentication authentication,
            @PathVariable String id
    ){
        TrecAuthentication trecAuthentication = (TrecAuthentication) authentication;
        return imageWriteService.cancelDelete(
                trecAuthentication.getUser(),
                trecAuthentication.getBrand(),
                id
        ).map(ResponseObj::toEntity);
    }


}
