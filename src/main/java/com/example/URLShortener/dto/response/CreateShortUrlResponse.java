package com.example.URLShortener.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CreateShortUrlResponse {

    @NotNull
    private LocalDateTime createdAt;

    @NotBlank
    private String originUrl;

    @NotBlank
    private String shortUrl;

    private LocalDateTime expiresAt;
}
