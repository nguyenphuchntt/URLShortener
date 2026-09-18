package com.example.URLShortener.controller;

import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.exception.ResourceNotFoundException;
import com.example.URLShortener.service.ShortUrlService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Validated
public class RedirectController {

    private final ShortUrlService shortUrlService;

    @GetMapping("${short-code.redirect-path}{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable @Size(max = 16) @Pattern(regexp = "^[a-zA-Z0-9]+$") String shortCode) {
        ShortUrl shortUrl = shortUrlService.getByCodeForRedirect(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short code not found"));
        return ResponseEntity
                .status(HttpStatus.FOUND) // 302
                .location(URI.create(shortUrl.getOriginUrl()))
                .build();
    }
}
