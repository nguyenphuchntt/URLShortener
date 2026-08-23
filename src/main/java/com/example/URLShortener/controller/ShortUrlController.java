package com.example.URLShortener.controller;

import com.example.URLShortener.dto.request.CreateShortUrlRequest;
import com.example.URLShortener.dto.response.CreateShortUrlResponse;
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.mapper.ShortUrlMapper;
import com.example.URLShortener.service.ShortUrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
public class ShortUrlController {

    private final ShortUrlService shortUrlService;
    private final ShortUrlMapper shortUrlMapper;

    public ShortUrlController(ShortUrlService shortUrlService,
                              ShortUrlMapper shortUrlMapper) {
        this.shortUrlService = shortUrlService;
        this.shortUrlMapper = shortUrlMapper;
    }

    @PostMapping
    public ResponseEntity<CreateShortUrlResponse> createShortUrl(@RequestBody @Valid CreateShortUrlRequest createShortUrlRequest) {
        ShortUrl shortUrl = shortUrlService.create(createShortUrlRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(shortUrlMapper.toResponse(shortUrl));
    }
}
