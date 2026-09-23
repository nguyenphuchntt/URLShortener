package com.example.URLShortener.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GET /analytics/{shortUrlId}/by-country?days=30 — đọc click_country_stats theo range ngày.
 */
@Data
@Builder
public class CountryClicksResponse {
    private Long shortUrlId;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String country;
        private long clicks;
    }
}
