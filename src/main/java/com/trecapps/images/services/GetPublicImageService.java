package com.trecapps.images.services;

import com.trecapps.images.models.*;
import com.trecapps.images.repos.ImageRepo;
import com.trecapps.images.repos.ProfileRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.List;

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

                    if(!ImageState.PUBLIC.equals(record.getState()))
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
                                MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
                                String type = record.getType();
                                if(!type.startsWith("image/"))
                                    type = "image/" + type;
                                headers.add("Content-Type", type);
                                return new ResponseEntity<byte[]>(bytes, headers, HttpStatusCode.valueOf(200));
                            });
                });
    }

    public Mono<ResponseEntity<byte[]>> getImageById(String id, String crop){
            return getImage(id, crop)
                .onErrorResume(ObjectResponseException.class, (ObjectResponseException ex) -> {
                    return Mono.just(new ResponseEntity<>(ex.getStatus()));
                });
    }

    public Mono<ResponseEntity<ResponseObj>> setProfile(String id, List<ImageProfileEntry> entries){
        ImageProfile iProfile = new ImageProfile();
        iProfile.setProfileId(id);
        iProfile.setEntries(entries);
        return profileRepo.save(iProfile).thenReturn(ResponseObj.getInstance("Success", id))
                .map(ResponseObj::toEntity);
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

                    // If the target app wasn't main yet it was not found, fallback to main
                    if(!"main".equals(tApp))
                    {
                        tApp = "main";
                        for(ImageProfileEntry appEntry: iProfile.getEntries()){
                            if(tApp.equals(appEntry.getApp()))
                                return appEntry.getImageId();
                        }
                    }
                                        
                    throw new ObjectResponseException("Profile App Entry not found", HttpStatus.NOT_FOUND);
                })
                .flatMap((String id) -> getImage(id, null))
                .onErrorResume(ObjectResponseException.class, (ObjectResponseException ex) -> {
                    return Mono.just(new ResponseEntity<>(ex.getStatus()));
                });

    }


}
