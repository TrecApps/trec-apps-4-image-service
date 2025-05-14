package com.trecapps.images.services;

import com.trecapps.auth.common.models.TcBrands;
import com.trecapps.auth.common.models.TcUser;
import com.trecapps.images.models.*;
import com.trecapps.images.repos.ImageRepo;
import com.trecapps.images.repos.ProfileRepo;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ImageWriteService {

    @Value("${trecapps.image.soft-delete-days:30}")
    int softDeleteDays;

    @Autowired
    IImageStorageService imageStorageService;

    @Autowired
    ImageRepo imageRepo;

    @Autowired
    ProfileRepo profileRepo;

    List<String> validHeaderTypes = List.of(
            "image/gif",
            "image/bmp",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/apng",
            "image/avif"
    );

    String filterImageType(String type){
        String t = type.trim().toLowerCase(Locale.ROOT);
        if(!validHeaderTypes.contains(t))
            throw new ObjectResponseException("Invalid Image type reported!", HttpStatus.BAD_REQUEST);
        return t;
    }


    ImageState getState(String state) {
        return switch (state) {
            case "uploaded" -> ImageState.UPLOADED;
            case "prePublic" -> ImageState.PRE_PUBLIC;
            case "preProfile" -> ImageState.PRE_PROFILE;
            default -> throw new ObjectResponseException(
                    "Invalid State mode provided! Needs to be 'uploaded', 'prePublic', 'preProfile' or null (uploaded)",
                    HttpStatus.BAD_REQUEST);
        };
    }

    public Mono<ResponseObj> postImage(
            @NotNull TcUser user,
            @Nullable TcBrands brands,
            @NotNull byte[] pictureData,
            @NotNull String typeHeader,
            @Nullable String crop,
            @Nullable String mode,
            @Nullable String album,
            @Nullable String app,
            @Nullable String name
    ){
        return Mono.just(pictureData)
                .flatMap((byte[] data) -> {
                    String uploadMode = mode;
                    if(uploadMode == null)
                        uploadMode = "uploaded";
                    BufferedImage sourceImage;
                    try {
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
                        sourceImage = ImageIO.read(inputStream);
                        inputStream.close();


                    } catch (IOException e) {
                        throw new ObjectResponseException("Error Reading image", HttpStatus.INTERNAL_SERVER_ERROR);
                    }


                    Set<String> albums = album == null ? new HashSet<>() : new HashSet<String>(List.of(album.split(";")));

                    ImageRecord imageRecord = new ImageRecord();
                    imageRecord.setState(getState(uploadMode));
                    imageRecord.setId(UUID.randomUUID().toString());
                    imageRecord.setApp(app);
                    imageRecord.setAlbum(albums);
                    imageRecord.setName(name);
                    imageRecord.setOwner(
                            brands == null ?
                                String.format("User-%s", user.getId()) :
                                String.format("Brand-%s", brands.getId())
                    );

                    imageRecord.setType(filterImageType(typeHeader));
                    imageRecord.setHeight(sourceImage.getHeight());
                    imageRecord.setWidth(sourceImage.getWidth());

                    if(imageStorageService.canUseCrop(crop, imageRecord))
                        imageRecord.setDefaultCrop(crop);

                    return imageRepo.save(imageRecord)
                            .flatMap((ImageRecord ir) -> {
                                return this.imageStorageService.persistImage(ir, data)
                                        .thenReturn(ResponseObj.getInstance("Success", imageRecord.getId()));
                            });

                })
                .onErrorResume(ObjectResponseException.class, (ObjectResponseException ex) -> Mono.just(ex.toResponseObj()));
    }

    Mono<ResponseObj> updateVisibility(ImageRecord record, ImageVisibility visibility){
        switch(visibility){
            case PUBLIC:
            {
                if(ImageState.PUBLIC.equals(record.getState()))
                    return Mono.just(ResponseObj.getInstance(HttpStatus.ALREADY_REPORTED, "Already public!"));
                if(!record.isPublicEligible())
                    throw new ObjectResponseException("Image not marked for Public availability!", HttpStatus.PRECONDITION_FAILED);

                // Make the image public
                record.setState(ImageState.PUBLIC);
                return this.imageStorageService.transferImage(record)
                        .flatMap((ResponseObj obj) -> {
                            return this.imageRepo.save(record).thenReturn(obj);
                        });
            }
            case PUBLIC_AUTH:
                case PROTECTED:
            {
                Mono<ResponseObj> ret = ImageState.PUBLIC.equals(record.getState()) ?
                    Mono.just(record).flatMap((ImageRecord r) -> {
                        r.setState(ImageState.NON_ADULT);
                        r.setAllowPublic(visibility.equals(ImageVisibility.PUBLIC_AUTH));
                        return this.imageStorageService.transferImage(record);

                    }).flatMap((ResponseObj obj) -> {
                        return this.imageRepo.save(record).thenReturn(obj);
                    })
                :  Mono.just(record).flatMap((ImageRecord r) -> {
                    r.setAllowPublic(visibility.equals(ImageVisibility.PUBLIC_AUTH));
                    return this.imageRepo.save(r).thenReturn(ResponseObj.getInstance(HttpStatus.OK, "Success!"));

                });
                return ret;
            }

        }
        throw new ObjectResponseException("Visibility not detected!", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public Mono<ResponseObj> patchData(
            @NotNull TcUser user,
            @Nullable TcBrands brands,
            @NotNull String imageId,
            ImagePatch patch
    ){
        return imageRepo.findById(imageId)
                .doOnNext((ImageRecord imageRecord) -> {
                    if(imageRecord == null)
                        throw new ObjectResponseException("Image not found!", HttpStatus.NOT_FOUND);

                    String profile = HelperMethods.getRequesterProfile(user, brands);
                    if(!profile.equals(imageRecord.getOwner()))
                        throw new ObjectResponseException("Image does not belong to you!", HttpStatus.FORBIDDEN);
                })
                .flatMap((ImageRecord record) -> {
                    switch(patch.getField()){
                        case "crop":
                            if(patch.getValue() == null)
                                record.setDefaultCrop(null);
                            else
                            {
                                String newCrop = patch.getValue().trim();

                                if(!imageStorageService.canUseCrop(patch.getValue(), record))
                                    throw new ObjectResponseException(
                                            "'crop' field requires a value of the format '[x],[y],[width],[height]'" +
                                                    " for which the resulting rectangle fits on the image!",
                                            HttpStatus.BAD_REQUEST);
                                record.setDefaultCrop(newCrop);
                            }
                            break;
                        case "album":
                        {
                            String value = patch.getValue();
                            if(value == null)
                                record.setAlbum(new HashSet<>());
                            else {
                                value = value.trim();
                                record.setAlbum(new HashSet<>(List.of(value)));
                            }
                        }
                        break;
                        case "visibility": {

                            ImageVisibility visibility = null;
                            try {
                                visibility = ImageVisibility.valueOf(patch.getValue());
                            } catch (IllegalArgumentException ignore) {
                                throw new ObjectResponseException("Visibility updates need to be specified with 'PUBLIC', 'PUBLIC_AUTH', or 'PROTECTED'", HttpStatus.BAD_REQUEST);
                            }

                            return updateVisibility(record, visibility);
                        }
                        case "owner":
                            throw new ObjectResponseException("Field 'owner' not supported at this time! Use 'crop' or 'album'!", HttpStatus.NOT_IMPLEMENTED);
                        default:
                            throw new ObjectResponseException("Unrecognized field update! Needs to be 'crop' or 'album'!", HttpStatus.BAD_REQUEST);
                    }

                    return this.imageRepo.save(record)
                            .thenReturn(ResponseObj.getInstance(HttpStatus.OK, "Success"));
                })
                .onErrorResume(ObjectResponseException.class, (ObjectResponseException ex) -> Mono.just(ex.toResponseObj()));
    }


    public Mono<ResponseObj> patchReaders(
            @NotNull TcUser user,
            @Nullable TcBrands brands,
            @NotNull String imageId,
            boolean setPrivate,
            ReaderAction action
    ){
        return imageRepo.findById(imageId)
                .doOnNext((ImageRecord imageRecord) -> {
                    if(imageRecord == null)
                        throw new ObjectResponseException("Image not found!", HttpStatus.NOT_FOUND);

                    String profile = HelperMethods.getRequesterProfile(user, brands);
                    if(!profile.equals(imageRecord.getOwner()))
                        throw new ObjectResponseException("Image does not belong to you!", HttpStatus.FORBIDDEN);
                })
                .flatMap((ImageRecord imageRecord) -> {
                    Set<String> currentReaders = imageRecord.getReaders();
                    if(action.getAction().equals(ReaderActionType.ADD))
                        currentReaders.addAll(action.getReaders());
                    else {
                        action.getReaders().forEach(currentReaders::remove);
                    }
                    boolean transferToPrivate;
                    if(setPrivate && ImageState.PUBLIC.equals(imageRecord.getState())){
                        transferToPrivate = true;
                        imageRecord.setState(ImageState.NON_ADULT);
                    } else {
                        transferToPrivate = false;
                    }

                    return imageRepo.save(imageRecord)
                            .flatMap((ImageRecord ir) -> {
                                if(!transferToPrivate){
                                    // ToDo Return
                                    return Mono.just(ResponseObj.getInstance(HttpStatus.OK, "Success"));
                                }
                                // Here, we are making the transfer
                                ir.setState(ImageState.NON_ADULT);
                                return this.imageStorageService.transferImage(ir)
                                        .flatMap((ResponseObj obj) -> {
                                            if(obj.getHttpStatus().is2xxSuccessful())
                                                return imageRepo.save(ir).thenReturn(obj);
                                            return Mono.just(obj);
                                        });

                            });
                })
                .onErrorResume(ObjectResponseException.class, (ObjectResponseException ex) -> Mono.just(ex.toResponseObj()));

    }

    public Mono<ResponseObj> setProfilePic(
            @NotNull TcUser user,
            @Nullable TcBrands brands,
            @NotNull String imageId,
            @Nullable String app
    ){
        String profile = HelperMethods.getRequesterProfile(user, brands);
        return imageRepo.findById(imageId)
                .doOnNext((ImageRecord imageRecord) -> {
                    if(imageRecord == null)
                        throw new ObjectResponseException("Image not found!", HttpStatus.NOT_FOUND);


                    if(!profile.equals(imageRecord.getOwner()))
                        throw new ObjectResponseException("Image does not belong to you!", HttpStatus.FORBIDDEN);

                    if(ImageState.ADULT.equals(imageRecord.getState()))
                        throw new ObjectResponseException("Adult images cannot be used as Profile Pictures!", HttpStatus.FORBIDDEN);

                    switch(imageRecord.getState()){
                        case ERROR:
                        case ImageState.UPLOADED:
                        case ImageState.PRE_PUBLIC:
                        case ImageState.PRE_PROFILE:
                        throw new ObjectResponseException("Image not in a state where it can be marked as a profile picture", HttpStatus.PRECONDITION_FAILED);
                    }


                })
                .flatMap((ImageRecord imageRecord) -> {

                    if(imageRecord.getState().equals(ImageState.NON_ADULT)) {
                        imageRecord.setState(ImageState.PUBLIC);
                        return this.imageStorageService.transferImage(imageRecord).thenReturn(imageRecord);

                    }
                    return Mono.just(imageRecord);

                })
                .flatMap((ImageRecord imageRecord) -> {
                    return HelperMethods.setProfileImage(profileRepo, imageRecord, profile, app);
                })
                .onErrorResume(ObjectResponseException.class, (ObjectResponseException ex) -> Mono.just(ex.toResponseObj()));
    }

    public Mono<List<ImageRecord>> getImageList(
            @NotNull TcUser user,
            @Nullable TcBrands brands,
            @Nullable String app,
            @Nullable String album,
            int page,
            int size
    ) {
        String profile = HelperMethods.getRequesterProfile(user, brands);
        Pageable pageable = PageRequest.of(page, size);
        Flux<ImageRecord> records;
        if(app == null){
            records = album == null ?
                    imageRepo.findByOwner(profile, pageable) :
                    imageRepo.findByOwnerAndAlbum(profile, album, pageable);
        } else {
            records = album == null ?
                    imageRepo.findByOwnerAndApp(profile, app, pageable) :
                    imageRepo.findByOwnerAppAndAlbum(profile, app, album, pageable);
        }

        return records.collectList();
    }


    public Mono<ResponseObj> getImageAsBase64(
            @NotNull TcUser user,
            @Nullable TcBrands brands,
            @NotNull String imageId,
            @Nullable String crop,
            boolean allowAdult
    ) {
        String profile = HelperMethods.getRequesterProfile(user, brands);
        return imageRepo.findById(imageId)
                .doOnNext((ImageRecord imageRecord) -> {
                    if(imageRecord == null)
                        throw new ObjectResponseException("Image not found!", HttpStatus.NOT_FOUND);

                    if(!ImageState.PUBLIC.equals(imageRecord.getState()) && !imageRecord.isAllowPublic()) {
                        if(!profile.equals(imageRecord.getOwner()) && !imageRecord.getReaders().contains(profile))
                            throw new ObjectResponseException("Access denied!", HttpStatus.FORBIDDEN);
                    }

                    if(ImageState.ADULT.equals(imageRecord.getState())){
                        if(!allowAdult || !user.getBirthday().plusYears(18).isAfter(OffsetDateTime.now()))
                            throw new ObjectResponseException("Adult Image denied!", HttpStatus.FORBIDDEN);
                    }



                })
                .flatMap((ImageRecord imageRecord) -> {
                    String useCrop = crop;
                    if(useCrop == null)
                        useCrop = imageRecord.getDefaultCrop();
                    else if(useCrop.equals("whole"))
                        useCrop = null;

                    return this.imageStorageService.retrieveImageBase64(imageRecord, useCrop);
                })
                .map((String base64) -> ResponseObj.getInstance(HttpStatus.OK, base64))
                .onErrorResume(ObjectResponseException.class, (ObjectResponseException ex) -> Mono.just(ex.toResponseObj()));
    }


    public Mono<ResponseObj> startDelete(
            @NotNull TcUser user,
            @Nullable TcBrands brands,
            @NotNull String imageId){
        String profile = HelperMethods.getRequesterProfile(user, brands);
        return imageRepo.findById(imageId)
                .doOnNext((ImageRecord imageRecord) -> {
                    if(imageRecord == null)
                        throw new ObjectResponseException("Image not found!", HttpStatus.NOT_FOUND);

                    if(!profile.equals(imageRecord.getOwner()))
                        throw new ObjectResponseException("Image does not belong to you!", HttpStatus.FORBIDDEN);

                    OffsetDateTime deleteOn = imageRecord.getDeleteOn();
                    OffsetDateTime now = OffsetDateTime.now();
                    if(deleteOn != null){
                        long daysToDeletion = Math.abs(ChronoUnit.DAYS.between(deleteOn, now));
                        if(daysToDeletion == 0)
                            throw new ObjectResponseException("Deletion already submitted! Your image will be deleted later today!", HttpStatus.ALREADY_REPORTED);
                        if(daysToDeletion == 1)
                            throw new ObjectResponseException("Deletion already submitted! Image will be deleted in 1 day!", HttpStatus.ALREADY_REPORTED);
                        throw new ObjectResponseException(String.format("Deletion already submitted! Image will be deleted in %d days!", daysToDeletion), HttpStatus.ALREADY_REPORTED);

                    }

                    imageRecord.setDeleteOn(now.plus(softDeleteDays, ChronoUnit.DAYS));
                })
                .flatMap((ImageRecord imageRecord) -> {
                    return this.imageRepo.save(imageRecord)
                            .map((ImageRecord ir) -> {
                                ResponseObj obj = ResponseObj.getInstance(HttpStatus.OK,
                                        String.format("Deletion Request submitted. Your image will be deleted in %d days!", this.softDeleteDays));
                                obj.setId(ir.getDeleteOn().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                                return obj;
                            });

                })
                .onErrorResume((Throwable thrown) -> {
                    return Mono.just(ResponseObj.getInstance(HttpStatus.INTERNAL_SERVER_ERROR, "Error Submitting Delete Request!"));
                });
    }

    public Mono<ResponseObj> cancelDelete(
            @NotNull TcUser user,
            @Nullable TcBrands brands,
            @NotNull String imageId
    ) {
        String profile = HelperMethods.getRequesterProfile(user, brands);
        return imageRepo.findById(imageId)
                .doOnNext((ImageRecord imageRecord) -> {
                    if (imageRecord == null)
                        throw new ObjectResponseException("Image not found!", HttpStatus.NOT_FOUND);

                    if (!profile.equals(imageRecord.getOwner()))
                        throw new ObjectResponseException("Image does not belong to you!", HttpStatus.FORBIDDEN);

                    OffsetDateTime deleteOn = imageRecord.getDeleteOn();
                    if (deleteOn == null)
                        throw new ObjectResponseException("Image not marked for Deletion!", HttpStatus.PRECONDITION_FAILED);
                    imageRecord.setDeleteOn(null);
                }).flatMap((ImageRecord imageRecord) -> {
                    return this.imageRepo.save(imageRecord)
                            .thenReturn(
                                    ResponseObj.getInstance(HttpStatus.OK,
                                            "Cancellation Request submitted. Your image will not be deleted!"));
                })
                .onErrorResume((Throwable thrown) -> {
                    return Mono.just(ResponseObj.getInstance(HttpStatus.INTERNAL_SERVER_ERROR, "Error Submitting Delete Request!"));
                });
    }
}
