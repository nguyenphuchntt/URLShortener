package com.example.URLShortener.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * GET /analytics/{shortUrlId}/summary — đọc thẳng link_stats (1 row).
 */
@Data
@Builder
public class LinkSummaryResponse {
    private Long shortUrlId;
    private String shortCode;
    private long totalClicks;
    private Instant lastClickAt;
}
