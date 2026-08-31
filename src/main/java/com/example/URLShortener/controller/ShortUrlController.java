package com.example.URLShortener.controller;

import com.example.URLShortener.dto.request.CreateShortUrlRequest;
import com.example.URLShortener.dto.request.UpdateShortUrlRequest;
import com.example.URLShortener.dto.response.CreateShortUrlResponse;
import com.example.URLShortener.dto.response.ShortUrlResponse;
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.exception.ResourceNotFoundException;
import com.example.URLShortener.service.ShortUrlService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/urls")
@Validated
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

    @PatchMapping
    public ResponseEntity<ShortUrlResponse> updateShortUrl (
            @RequestBody @Valid UpdateShortUrlRequest request) {
        ShortUrl shortUrl = shortUrlService.update(request)
                .orElseThrow(() -> new ResourceNotFoundException("Short code not found"));
        return ResponseEntity.status(HttpStatus.OK).body(
                ShortUrlResponse.builder()
                        .shortCode(shortUrl.getShortCode())
                        .updatedAt(shortUrl.getUpdatedAt())
                        .expiresAt(shortUrl.getExpiresAt())
                        .originUrl(shortUrl.getOriginUrl())
                        .status(shortUrl.getStatus())
                        .build()
        );
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable("shortCode")
            @Size(max = 16)
            @Pattern(regexp = "^[a-zA-Z0-9]+$")
            String shortCode) {

        ShortUrl shortUrl = shortUrlService.getByCodeForRedirect(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short code not found"));

        return ResponseEntity
                .status(HttpStatus.FOUND) // 302
                .location(URI.create(shortUrl.getOriginUrl()))
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<Page<ShortUrlResponse>> getMyUrls(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(
            shortUrlService.getMyUrls(pageable)
        );
    }

    @DeleteMapping("{shortCode}")
    public ResponseEntity<ShortUrlResponse> deleteShortCode(
            @PathVariable("shortCode") @Size(max = 16) @Pattern(regexp = "^[a-zA-Z0-9]+$") String shortCode) {
        ShortUrl url =shortUrlService.delete(shortCode);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(
                ShortUrlResponse.builder()
                        .shortCode(url.getShortCode())
                        .status(url.getStatus())
                        .updatedAt(url.getUpdatedAt())
                        .expiresAt(url.getExpiresAt())
                        .build()
        );
    }
}
