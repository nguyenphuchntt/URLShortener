package com.example.URLShortener.dto.response;

import com.example.URLShortener.entity.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private LocalDateTime time;
    private int status;
    private String errorMessage;
    private ErrorCode errorCode;
    private String path;
}
