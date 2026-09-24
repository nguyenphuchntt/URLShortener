package com.example.URLShortener.controller;

import com.example.URLShortener.analytic.ClickEventPayload;
import com.example.URLShortener.analytic.ClickEventPublisher;
import com.example.URLShortener.cache.BloomFilterService;
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.exception.ResourceNotFoundException;
import com.example.URLShortener.service.ShortUrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class RedirectController {

    private final ShortUrlService shortUrlService;
    private final ClickEventPublisher clickEventPublisher;
    private final BloomFilterService bloomFilterService;

    @GetMapping("${short-code.redirect-path}{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable @Size(max = 16) @Pattern(regexp = "^[a-zA-Z0-9]+$") String shortCode,
            HttpServletRequest request) {
        // Bloom filter check
        if (!bloomFilterService.mightContain(shortCode)) {
            log.debug("Bloom filter rejected unknown shortCode={}", shortCode);
            throw new ResourceNotFoundException("Short code not found");
        }
        ShortUrl shortUrl = shortUrlService.getByCodeForRedirect(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short code not found"));
        // Async: publish an event to Redis stream for click analysis
        clickEventPublisher.publishClickEvent(ClickEventPayload.builder()
                .shortUrlId(shortUrl.getId())
                .ip(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .referrer(request.getHeader("Referer"))
                .timestamp(System.currentTimeMillis())
                .build());

        return ResponseEntity
                .status(HttpStatus.FOUND) // 302
                .location(URI.create(shortUrl.getOriginUrl()))
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
