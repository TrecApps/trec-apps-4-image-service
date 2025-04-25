package com.trecapps.images.models;

import com.trecapps.auth.common.models.TcBrands;
import com.trecapps.auth.common.models.TcUser;
import jakarta.annotation.Nullable;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;

@Data
@Document("Record")
public class Record implements Comparable<Record> {

    @Id
    String id;
    String falsehoodId; // Used when stored as separate entities

    String userId;
    String brandId;
    String displayName;
    OffsetDateTime date;
    RecordEvent event;

    String comment;

    Integer points;

    public void setMaker(@NotNull TcUser user, @Nullable TcBrands brands){
        this.userId = user.getId();
        if(brands == null)
            this.displayName = user.getDisplayName();
        else {
            this.displayName = brands.getName();
            this.brandId = brands.getId();
        }
    }

    @Override
    public int compareTo(Record o) {
        return date.compareTo(o.date);
    }
}
