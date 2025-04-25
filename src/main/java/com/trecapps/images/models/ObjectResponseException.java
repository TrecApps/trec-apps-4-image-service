package com.trecapps.images.models;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public class ObjectResponseException extends RuntimeException {

    @Getter
    HttpStatus status;
    public ObjectResponseException(String message, HttpStatus status1) {
        super(message);
        this.status = status1;
    }

    public ResponseObj toResponseObj(){
      return ResponseObj.getInstance(status, getMessage());
    }
}
