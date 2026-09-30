package com.example.URLShortener.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "Số click của một link theo từng giờ trong khoảng giờ gần nhất")
@Data
@Builder
public class HourlyClicksResponse {

    @Schema(description = "Internal ID of the link being analyzed", example = "42")
    private Long shortUrlId;

    @Schema(description = "Data points sorted in ascending chronological order")
    private List<Item> items;

    // Explicit schema name — see the explanation in ClickTimeseriesResponse.Item
    @Schema(name = "HourlyClickPoint", description = "Số click trong một giờ")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @Schema(description = "Corresponding hour in ISO-8601 format", example = "2026-09-26T14:00:00")
        private String hour;

        @Schema(description = "Clicks during that hour", example = "9")
        private long clicks;
    }
}
