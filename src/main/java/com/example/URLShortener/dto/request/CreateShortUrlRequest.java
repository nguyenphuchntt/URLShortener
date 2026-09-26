package com.example.URLShortener.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Data
public class CreateShortUrlRequest {

    @NotBlank
    @URL
    private String originUrl;

    @Size(min = 6, max = 16)
    @Pattern(regexp = "^[a-zA-Z0-9]+$")
    private String customShortCode;

    @Future
    private LocalDateTime expiresAt;
}
