package com.example.URLShortener.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "Click count for a link by country over the most recent period")
@Data
@Builder
public class CountryClicksResponse {

    @Schema(description = "Internal ID of the link being analyzed", example = "42")
    private Long shortUrlId;

    @Schema(description = "Countries with clicks, sorted by click count in descending order")
    private List<Item> items;

    // Explicit schema name — see the explanation in ClickTimeseriesResponse.Item
    @Schema(name = "CountryClickPoint", description = "Click count from a country")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @Schema(description = """
                ISO-3166 alpha-2 country code, or `UNKNOWN` when the IP address cannot be resolved
                (localhost, private IP, or an IP address not present in GeoLite2).
                """,
                example = "VN")
        private String country;

        @Schema(description = "Click count from that country", example = "84")
        private long clicks;
    }
}
