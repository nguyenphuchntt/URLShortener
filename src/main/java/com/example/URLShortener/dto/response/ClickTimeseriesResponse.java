package com.example.URLShortener.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "Account-wide click time series grouped by `granularity`")
@Data
@Builder
public class ClickTimeseriesResponse {

    @Schema(description = "Data points sorted in ascending chronological order")
    private List<Item> items;

    // Explicit schema names: all three analytics DTOs have a nested class named `Item`; if left as default,
    // springdoc merges them into one `Item` schema and two of the three endpoints display incorrect fields.
    @Schema(name = "Time seriesPoint", description = "Một mốc thời gian trong chuỗi click")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @Schema(description = "Corresponding date in ISO `yyyy-MM-dd` format", example = "2026-09-26")
        private String date;

        @Schema(description = "Clicks on that day", example = "37")
        private long clicks;
    }
}
