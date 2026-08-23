package com.example.URLShortener.dto.response;

import com.example.URLShortener.entity.enums.ShortUrlStatus;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Data
public class ShortUrlResponse {
    String shortCode;

    @URL
    private String originUrl;

    private ShortUrlStatus status;

    private LocalDateTime expiresAt;
}
