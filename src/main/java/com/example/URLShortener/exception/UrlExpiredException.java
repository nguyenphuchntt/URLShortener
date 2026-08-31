package com.example.URLShortener.exception;

import com.example.URLShortener.entity.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class UrlExpiredException extends AppException {
    public UrlExpiredException(String message) {
        super(ErrorCode.URL_EXPIRED, message, HttpStatus.GONE);
    }
}
