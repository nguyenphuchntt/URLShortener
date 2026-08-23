package com.example.URLShortener.dto.response;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CreateShortUrlResponse {

    @NotNull
    private LocalDateTime createdAt;

    private String originUrl;

    private String shortUrl;

    private LocalDateTime expiresAt;
}
