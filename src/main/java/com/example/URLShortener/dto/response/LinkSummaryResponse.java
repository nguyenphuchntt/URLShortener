package com.example.URLShortener.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Schema(description = "Summary metrics for a short link")
@Data
@Builder
public class LinkSummaryResponse {

    @Schema(description = "Internal link ID (not the short code)", example = "42")
    private Long shortUrlId;

    @Schema(description = "Short code của link", example = "u3Kf9a")
    private String shortCode;

    @Schema(description = "Total clicks for the link", example = "128")
    private long totalClicks;

    @Schema(description = "Most recent click time; `null` if no clicks have been recorded",
            example = "2026-09-26T14:03:11Z")
    private Instant lastClickAt;
}
