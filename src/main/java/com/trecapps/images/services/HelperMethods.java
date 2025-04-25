package com.trecapps.images.services;

import com.trecapps.auth.common.models.TcBrands;
import com.trecapps.auth.common.models.TcUser;
import com.trecapps.images.models.ImageProfile;
import com.trecapps.images.models.ImageProfileEntry;
import com.trecapps.images.models.ImageRecord;
import com.trecapps.images.models.ResponseObj;
import com.trecapps.images.repos.ProfileRepo;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelperMethods {

    static final Pattern CROP_PATTERN = Pattern.compile("[0-9]+,[0-9]+,[0-9]+,[0-9]+");

    public static boolean isCropFormatted(String submission)
    {
        Matcher match = CROP_PATTERN.matcher(submission);
        return match.matches();
    }

    public static String getRequesterProfile(@NotNull TcUser user, @Nullable TcBrands brand){
        return brand == null ?
            String.format("User-%s", user.getId()) :
            String.format("Brand-%s", brand.getId());
    }

    public static Mono<ResponseObj> setProfileImage(ProfileRepo profileRepo, ImageRecord imageRecord, String profile, String app){
        return profileRepo.findById(profile)
                .flatMap((ImageProfile imageProfile)-> {
                    if(imageProfile == null){
                        imageProfile = new ImageProfile();
                        imageProfile.setProfileId(profile);
                        imageProfile.setEntries(new ArrayList<>());
                    }

                    List<ImageProfileEntry> entries = imageProfile.getEntries();

                    ImageProfileEntry targetEntry = null;
                    String targetApp = app;
                    if(targetApp == null)
                        targetApp = "main";

                    for(ImageProfileEntry entry: entries){
                        if(targetApp.equals(entry.getApp())){
                            targetEntry = entry;
                            break;
                        }
                    }

                    if(targetEntry == null){
                        targetEntry = new ImageProfileEntry();
                        targetEntry.setApp(targetApp);
                        entries.add(targetEntry);
                    }

                    targetEntry.setType(imageRecord.getType());
                    targetEntry.setImageId(imageRecord.getId());

                    return profileRepo.save(imageProfile).thenReturn(ResponseObj.getInstance(HttpStatus.OK, "Success"));
                });
    }
}
