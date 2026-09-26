package com.example.URLShortener.controller;

import com.example.URLShortener.dto.response.AnalyticsOverviewResponse;
import com.example.URLShortener.dto.response.ClickTimeseriesResponse;
import com.example.URLShortener.dto.response.CountryClicksResponse;
import com.example.URLShortener.dto.response.HourlyClicksResponse;
import com.example.URLShortener.dto.response.LinkSummaryResponse;
import com.example.URLShortener.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> getAccountOverview() {
        return ResponseEntity.ok(analyticsService.getAccountOverview());
    }

    @GetMapping("/clicks")
    public ResponseEntity<ClickTimeseriesResponse> getAccountClicks(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String granularity) {
        return ResponseEntity.ok(analyticsService.getAccountClickTimeseries(from, to, granularity));
    }

    @GetMapping("/{shortUrlId}/summary")
    public ResponseEntity<LinkSummaryResponse> getSummary(@PathVariable Long shortUrlId) {
        return ResponseEntity.ok(analyticsService.getSummary(shortUrlId));
    }

    @GetMapping("/{shortUrlId}/hourly")
    public ResponseEntity<HourlyClicksResponse> getHourly(
            @PathVariable Long shortUrlId,
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(analyticsService.getHourlyClicks(shortUrlId, hours));
    }

    @GetMapping("/{shortUrlId}/by-country")
    public ResponseEntity<CountryClicksResponse> getByCountry(
            @PathVariable Long shortUrlId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getClicksByCountry(shortUrlId, days));
    }
}
