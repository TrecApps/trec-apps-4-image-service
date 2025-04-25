package com.trecapps.images.models;

import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Data
public class ResponseObj {

    int status;
    String message;
    String id;

    @Transient
    HttpStatus httpStatus;

    public static ResponseObj getInstance(String message1, String id){
        ResponseObj ret = getInstance(HttpStatus.CREATED, message1);
        ret.id = id;
        return ret;
    }

    public static ResponseObj getInstance(@NotNull HttpStatus status, String message1)
    {
        ResponseObj ret = new ResponseObj();
        ret.httpStatus = status;
        ret.status = status.value();
        ret.message = message1;
        return ret;
    }

    public ResponseEntity<ResponseObj> toEntity(){
        return new ResponseEntity<>(this, httpStatus);
    }
}
