package com.example.URLShortener.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Short link creation result")
@Data
@Builder
public class CreateShortUrlResponse {

    @Schema(description = "Link creation time", example = "2026-09-26T15:57:00.123")
    @NotNull
    private LocalDateTime createdAt;

    @Schema(description = "Original URL that was shortened", example = "https://example.com/very/long/path")
    @NotBlank
    private String originUrl;

    @Schema(description = "Newly generated short code (or the code requested through `customShortCode`)",
            example = "u3Kf9a")
    @NotBlank
    private String shortCode;

    @Schema(description = "Expiration time; `null` means the link never expires", example = "2027-01-01T00:00:00")
    private LocalDateTime expiresAt;
}
