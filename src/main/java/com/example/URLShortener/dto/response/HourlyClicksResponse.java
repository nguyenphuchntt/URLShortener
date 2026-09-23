package com.example.URLShortener.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GET /analytics/{shortUrlId}/hourly?hours=24 — đọc clicks_hourly theo range giờ.
 */
@Data
@Builder
public class HourlyClicksResponse {
    private Long shortUrlId;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String hour; // ISO-8601
        private long clicks;
    }
}
