package com.example.URLShortener.controller;

import com.example.URLShortener.dto.response.AnalyticsOverviewResponse;
import com.example.URLShortener.dto.response.ClickTimeseriesResponse;
import com.example.URLShortener.dto.response.CountryClicksResponse;
import com.example.URLShortener.dto.response.HourlyClicksResponse;
import com.example.URLShortener.dto.response.LinkSummaryResponse;
import com.example.URLShortener.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Analytics", description = "Click statistics")
@RestController
@RequestMapping(value = "/api/v1/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(
            summary = "Account overview",
            description = "Aggregate figures across all of the user's links: total links, total clicks, "
                    + "and the number of links in each status. Intended for dashboards."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Overview figures"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    })
    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> getAccountOverview() {
        return ResponseEntity.ok(analyticsService.getAccountOverview());
    }

    @Operation(
            summary = "Click time series",
            description = """
                    Clicks across all of the user's links, grouped by `granularity` between `from` and `to`.
                    `from` and `to` are ISO dates (`yyyy-MM-dd`) and are **required**; `from` must not be after `to`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Click time series"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    })
    @GetMapping("/clicks")
    public ResponseEntity<ClickTimeseriesResponse> getAccountClicks(
            @Parameter(description = "Start date (ISO date)", example = "2026-09-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (ISO date, inclusive)", example = "2026-09-26")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Granularity of the series", example = "day",
                    schema = @Schema(allowableValues = {"hour", "day", "month"}, defaultValue = "day"))
            @RequestParam(defaultValue = "day") String granularity) {
        return ResponseEntity.ok(analyticsService.getAccountClickTimeseries(from, to, granularity));
    }

    @Operation(
            summary = "Link summary",
            description = "Total clicks and the time of the most recent click for a single link. "
                    + "`shortUrlId` is the internal id, not the short code; another user's link → `404`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary figures for the link"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    @GetMapping("/{shortUrlId}/summary")
    public ResponseEntity<LinkSummaryResponse> getSummary(
            @Parameter(description = "Internal id of the short link", example = "42")
            @PathVariable Long shortUrlId) {
        return ResponseEntity.ok(analyticsService.getSummary(shortUrlId));
    }

    @Operation(
            summary = "Clicks by hour",
            description = "Clicks for a single link over the last `hours` hours, grouped by hour."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clicks per hour"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    @GetMapping("/{shortUrlId}/hourly")
    public ResponseEntity<HourlyClicksResponse> getHourly(
            @Parameter(description = "Internal id of the short link", example = "42")
            @PathVariable Long shortUrlId,
            @Parameter(description = "Number of most recent hours to include", example = "24")
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(analyticsService.getHourlyClicks(shortUrlId, hours));
    }

    @Operation(
            summary = "Clicks by country",
            description = """
                    Clicks for a single link over the last `days` days, grouped by country.
                    The country is derived from the clicker's IP via GeoLite2; IPs that cannot be
                    resolved (localhost, private IPs) are grouped under `UNKNOWN`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clicks per country"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    @GetMapping("/{shortUrlId}/by-country")
    public ResponseEntity<CountryClicksResponse> getByCountry(
            @Parameter(description = "Internal id of the short link", example = "42")
            @PathVariable Long shortUrlId,
            @Parameter(description = "Number of most recent days to include", example = "30")
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getClicksByCountry(shortUrlId, days));
    }
}
