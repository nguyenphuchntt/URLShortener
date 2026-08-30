package com.example.URLShortener.controller;

import com.example.URLShortener.dto.request.CreateShortUrlRequest;
import com.example.URLShortener.dto.response.CreateShortUrlResponse;
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.service.ShortUrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/urls")
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @PostMapping
    public ResponseEntity<CreateShortUrlResponse> createShortUrl(@RequestBody @Valid CreateShortUrlRequest request) {
        ShortUrl shortUrl = shortUrlService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            CreateShortUrlResponse.builder()
                    .createdAt(LocalDateTime.now())
                    .originUrl(request.getOriginUrl())
                    .shortCode(shortUrl.getShortCode())
                    .build()
        );
    }
}
