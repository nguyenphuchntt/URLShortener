package com.example.URLShortener.dto.response;

import com.example.URLShortener.entity.enums.ShortUrlStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Schema(description = "Short link information")
@Data
@Builder
public class ShortUrlResponse {

    @Schema(description = "Short code used to access the link", example = "u3Kf9a")
    @Size(min = 6, max = 16)
    @Pattern(regexp = "^[a-zA-Z0-9]+$")
    private String shortCode;

    @Schema(description = "Original URL targeted by the short code", example = "https://example.com/very/long/path")
    @URL
    private String originUrl;

    @Schema(description = "Current link status", example = "ACTIVE")
    private ShortUrlStatus status;

    @Schema(description = "Total recorded clicks", example = "128")
    private Long clicks;

    @Schema(description = "Link creation time", example = "2026-09-20T09:12:33")
    private LocalDateTime createdAt;

    @Schema(description = "Last update time", example = "2026-09-25T18:40:02")
    private LocalDateTime updatedAt;

    @Schema(description = "Expiration time; `null` means the link never expires", example = "2027-01-01T00:00:00")
    private LocalDateTime expiresAt;
}
