package com.example.URLShortener.exception;

import com.example.URLShortener.entity.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends AppException {

    public UnauthorizedException(String message) {
        this(ErrorCode.UNAUTHORIZED, message);
    }

    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.UNAUTHORIZED);
    }
}
