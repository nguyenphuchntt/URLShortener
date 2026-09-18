package com.example.URLShortener.controller;

import com.example.URLShortener.service.ShortUrlService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RedirectController {
    private final ShortUrlService shortUrlService;

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable @Size(max = 16) @Pattern(regexp = "^[a-zA-Z0-9]+$") String shortCode) {
        return shortUrlService.getByCodeForRedirect(shortCode)
                .map(url -> ResponseEntity.status(302).location(URI.create(url.getOriginUrl())).<Void>build())
                .orElseThrow(() -> new com.example.URLShortener.exception.ResourceNotFoundException("Short code not found"));
    }
}
