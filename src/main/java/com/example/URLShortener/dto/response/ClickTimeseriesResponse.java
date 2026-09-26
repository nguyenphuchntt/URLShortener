package com.example.URLShortener.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Account-wide click timeseries (aggregated across all user's links).
 * Matches frontend's TimeseriesResponse interface.
 */
@Data
@Builder
public class ClickTimeseriesResponse {
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String date;  // ISO-8601 date (YYYY-MM-DD)
        private long clicks;
    }
}
