package com.example.URLShortener.exception;

import com.example.URLShortener.entity.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class BadRequestException extends AppException {

    public BadRequestException(String message) {
        this(ErrorCode.INVALID_URL_FORMAT, message);
    }

    public BadRequestException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
    }
}
