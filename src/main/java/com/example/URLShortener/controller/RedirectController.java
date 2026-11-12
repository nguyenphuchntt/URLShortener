package com.example.URLShortener.controller;

import com.example.URLShortener.analytic.ClickEventPayload;
import com.example.URLShortener.analytic.ClickEventPublisher;
// import com.example.URLShortener.cache.BloomFilterService; // Disabled for performance testing.
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.exception.ResourceNotFoundException;
import com.example.URLShortener.service.ShortUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Links", description = "Create and manage short links")
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class RedirectController {

    private final ShortUrlService shortUrlService;
    private final ClickEventPublisher clickEventPublisher;
    // private final BloomFilterService bloomFilterService; // Disabled for performance testing.

    @Operation(
            summary = "Redirect to the original URL (302)",
            description = """
                    Returns `302` with a `Location` header pointing to the original URL, and records a
                    click event (asynchronously via a Redis Stream) for analytics.

                    **Note on the internal path:** when the short code is already cached, `RedirectFilter`
                    returns `302` directly and the request does **not** reach this controller; only a
                    cache miss gets here. The result seen by the client is the same in both branches
                    (the only difference is the `Cache-Control: no-store` header on the cache-hit branch).

                    **Error codes:** unknown short code → `404` (`URL_NOT_FOUND`); expired link
                    → `410` (`URL_EXPIRED`).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirects to the original URL",
                    headers = @Header(name = "Location", description = "The original URL to redirect to",
                            schema = @Schema(type = "string", format = "uri"))),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "410", ref = "#/components/responses/Gone")
    })
    @GetMapping("${short-code.redirect-path}{shortCode}")
    public ResponseEntity<Void> redirect(
            @Parameter(description = "The short code to redirect", example = "u3Kf9a")
            @PathVariable @Size(max = 16) @Pattern(regexp = "^[a-zA-Z0-9]+$") String shortCode,
            HttpServletRequest request) {
        // Bloom filter check disabled for performance testing.
        // if (!bloomFilterService.mightContain(shortCode)) {
        //     log.debug("Bloom filter rejected unknown shortCode={}", shortCode);
        //     throw new ResourceNotFoundException("Short code not found");
        // }
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
