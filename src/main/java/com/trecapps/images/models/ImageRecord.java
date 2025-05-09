package com.trecapps.images.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Document("ImageRecord")
@Data
public class ImageRecord {

    static final Pattern CROP_PATTERN = Pattern.compile("[0-9]+,[0-9]+,[0-9]+,[0-9]+");
    static final List<String> allowedTypes = List.of(
            "gif",
            "jpeg",
            "png",
            "svg",
            "webp"
    );

    public static final String BOOL_FIELD_ADULT = "isAdultContent";
    public static final String BOOL_FIELD_GORY = "isGoryContent";


    public static int[] convertCrop(String crop){
        String[] cropDimensions = crop.split(",");
        if(cropDimensions.length != 4){
            throw new ObjectResponseException(String.format("'%s' needs to have four numbers divided by ','!", crop), HttpStatus.BAD_REQUEST);

        }

        try{
            List<Integer> dimensions = Arrays.stream(cropDimensions).map((String s) -> Integer.parseInt(s.trim())).toList();
            int[] ret = new int[dimensions.size()];
            for(int c = 0; c < ret.length; c++){
                ret[c] = dimensions.get(c);
                if(ret[c] < 0)
                    throw new ObjectResponseException("Crop dimensions cannot have a negative value!", HttpStatus.BAD_REQUEST);
            }
            return ret;
        } catch (NumberFormatException ignore){
            throw new ObjectResponseException(String.format("'%s' needs to have four numbers divided by ','!", crop), HttpStatus.BAD_REQUEST);
        }
    }

    @JsonIgnore
    public boolean isPublicEligible(){
        if(analysis == null){
            // Assume it is not safe for publication
            return false;
        }



        // Make sure AI has verified that image is not adult or gory
        return !analysis.isAdultContent() && !analysis.isGoryContent();
    }


    @MongoId
    String id;
    String owner; // Either a user ("user-[user_id]") or a brand account ("brand-[brand_id]")
    Set<String> album = new TreeSet<>(); // the Albums the image belongs to
    String name; // Name of the image
    String app; // the app that uploaded this image
    String type; // the type of image
    ImageState state = ImageState.UPLOADED; // The state of the image entry
    OffsetDateTime uploaded; // when was the image uploaded
    String defaultCrop;     // the type of cropping that should be applied
    int width, height;  // The dimensions of the image (useful when checking the validity of the cropping)
    SortedSet<Record> records = new TreeSet<>();

    Set<String> readers = new HashSet<>(); // Who is allowed to read it (useful when restricting access)

    Analysis analysis;  // Extended properties that will be added via AI

    boolean allowPublic = false; // ignored unless image is marked as adult. then allows bypass access




    public void setDefaultCrop(String crop){
        if(crop == null){
            defaultCrop = null;
            return;
        }

        int[] dimensions = convertCrop(crop);

        if(dimensions[0] + dimensions[2] > width || dimensions[1] + dimensions[3] > height)
            throw new ObjectResponseException(
                    String.format("Crop overflow detected for image with %d width and %d height!", width, height), HttpStatus.BAD_REQUEST);
        this.defaultCrop = crop;
    }

    public void setType(String type){
        String t = type.toLowerCase(Locale.ROOT).trim();
        if(t.startsWith("image/"))
            t = t.substring(6);
        if(!allowedTypes.contains(t))
            throw new ObjectResponseException(
                    String.format("Image of type %s is not supported!", t), HttpStatus.BAD_REQUEST);
        this.type = t;
    }
}
