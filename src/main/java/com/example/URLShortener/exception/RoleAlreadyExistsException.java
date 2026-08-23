package com.example.URLShortener.exception;

import com.example.URLShortener.entity.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class RoleAlreadyExistsException extends AppException {

    public RoleAlreadyExistsException(String message) {
        super(
                ErrorCode.ROLE_ALREADY_EXISTS,
                message,
                HttpStatus.CONFLICT
        );
    }
}