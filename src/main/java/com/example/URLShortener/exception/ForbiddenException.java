package com.example.URLShortener.exception;

import com.example.URLShortener.entity.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends AppException {
    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message, HttpStatus.FORBIDDEN);
    }
}
