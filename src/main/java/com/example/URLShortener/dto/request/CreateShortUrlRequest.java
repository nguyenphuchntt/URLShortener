package com.example.URLShortener.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Data
public class CreateShortUrlRequest {

    @NotBlank
    @URL
    private String originUrl;

    @Size(max = 16)
    private String customShortCode;

    @Future
    private LocalDateTime expiresAt;
}
