package com.example.URLShortener.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "Account-wide overview metrics for the dashboard")
@Data
@Builder
public class AnalyticsOverviewResponse {

    @Schema(description = "Total links created", example = "42")
    private long totalLinks;

    @Schema(description = "Total clicks across all links", example = "1530")
    private long totalClicks;

    @Schema(description = "Links currently in ACTIVE status", example = "38")
    private long activeLinks;

    @Schema(description = "Links currently DISABLED", example = "2")
    private long disabledLinks;

    @Schema(description = "Links that have expired", example = "1")
    private long expiredLinks;

    @Schema(description = "Soft-deleted links", example = "1")
    private long deletedLinks;
}
