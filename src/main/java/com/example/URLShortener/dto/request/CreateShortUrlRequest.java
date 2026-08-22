package com.example.URLShortener.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class CreateShortUrlRequest {

    @NotBlank
    @URL
    private String originUrl;
}
