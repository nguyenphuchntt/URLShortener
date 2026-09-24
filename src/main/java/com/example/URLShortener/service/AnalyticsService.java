package com.example.URLShortener.service;

import com.example.URLShortener.dto.response.AnalyticsOverviewResponse;
import com.example.URLShortener.dto.response.ClickTimeseriesResponse;
import com.example.URLShortener.dto.response.CountryClicksResponse;
import com.example.URLShortener.dto.response.HourlyClicksResponse;
import com.example.URLShortener.dto.response.LinkSummaryResponse;

import java.time.LocalDate;

public interface AnalyticsService {

    AnalyticsOverviewResponse getAccountOverview();

    ClickTimeseriesResponse getAccountClickTimeseries(LocalDate from, LocalDate to, String granularity);

    LinkSummaryResponse getSummary(Long shortUrlId);

    HourlyClicksResponse getHourlyClicks(Long shortUrlId, int hours);

    CountryClicksResponse getClicksByCountry(Long shortUrlId, int days);
}
