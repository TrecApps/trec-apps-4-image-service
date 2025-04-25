package com.trecapps.images.services;

import com.trecapps.images.models.*;
import com.trecapps.images.repos.ImageRepo;
import com.trecapps.images.repos.ProfileRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@ConditionalOnProperty(prefix = "trecapps.image", value = "public-get", havingValue = "true")
public class GetPublicImageService {

    @Autowired
    IImageStorageService imageStorageService;

    @Autowired
    ImageRepo imageRepo;

    @Autowired
    ProfileRepo profileRepo;

    Mono<ResponseEntity<byte[]>> getImage(String id, String crop){
        return imageRepo.findById(id)
                .flatMap((ImageRecord record) -> {
                    if(record == null)
                        throw new ObjectResponseException("", HttpStatus.NOT_FOUND);

                    if(ImageState.PUBLIC.equals(record.getState()))
                        throw new ObjectResponseException("", HttpStatus.PRECONDITION_FAILED);

                    String useCrop = crop;
                    if("ignore".equals(useCrop))
                        useCrop = null;
                    else {
                        // Figure out the crop
                        if(useCrop == null)
                            useCrop = record.getDefaultCrop();
                    }

                    return this.imageStorageService.retrieveImage(record, useCrop)
                            .map((byte[] bytes) -> {
                                ResponseEntity<byte[]> response = ResponseEntity.ok(bytes);
                                response.getHeaders().add("Content-Type", record.getType());
                                return response;
                            });
                });
    }

    public Mono<ResponseEntity<byte[]>> getImageById(String id, String crop){
            return getImage(id, crop)
                .onErrorResume(ObjectResponseException.class, (ObjectResponseException ex) -> {
                    return Mono.just(new ResponseEntity<>(ex.getStatus()));
                });
    }

    public Mono<ResponseEntity<byte[]>> getImageByProfileId(String profileId, final String app) {
        return profileRepo.findById(profileId)
                .map((ImageProfile iProfile) -> {
                    if(iProfile == null)
                        throw new ObjectResponseException("Profile Entry not found!", HttpStatus.NOT_FOUND);
                    String tApp = app;
                    if(tApp == null)
                        tApp = "main";

                    for(ImageProfileEntry appEntry: iProfile.getEntries()){
                        if(tApp.equals(appEntry.getApp()))
                            return appEntry.getImageId();
                    }
                    throw new ObjectResponseException("Profile App Entry not found", HttpStatus.NOT_FOUND);
                })
                .flatMap((String id) -> getImage(id, null))
                .onErrorResume(ObjectResponseException.class, (ObjectResponseException ex) -> {
                    return Mono.just(new ResponseEntity<>(ex.getStatus()));
                });

    }


}
