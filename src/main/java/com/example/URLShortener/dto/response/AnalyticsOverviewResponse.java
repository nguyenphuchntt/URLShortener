package com.example.URLShortener.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Account-wide analytics overview for the dashboard.
 * Matches frontend's DashboardOverview interface.
 */
@Data
@Builder
public class AnalyticsOverviewResponse {
    private long totalLinks;
    private long totalClicks;
    private long activeLinks;
    private long disabledLinks;
    private long expiredLinks;
    private long deletedLinks;
}
