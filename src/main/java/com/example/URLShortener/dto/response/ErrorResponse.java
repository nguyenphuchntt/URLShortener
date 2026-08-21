package com.example.URLShortener.dto.response;

import com.example.URLShortener.entity.enums.ErrorCode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorResponse {

    LocalDateTime time;

    String errorMessage;

    ErrorCode errorCode;
}
