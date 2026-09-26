package com.example.URLShortener.service.impl;

import com.example.URLShortener.dto.response.AnalyticsOverviewResponse;
import com.example.URLShortener.dto.response.ClickTimeseriesResponse;
import com.example.URLShortener.dto.response.CountryClicksResponse;
import com.example.URLShortener.dto.response.HourlyClicksResponse;
import com.example.URLShortener.dto.response.LinkSummaryResponse;
import com.example.URLShortener.entity.ClickCountryStats;
import com.example.URLShortener.entity.ClickHourly;
import com.example.URLShortener.entity.LinkStats;
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.entity.enums.ErrorCode;
import com.example.URLShortener.entity.enums.ShortUrlStatus;
import com.example.URLShortener.exception.ResourceNotFoundException;
import com.example.URLShortener.exception.UnauthorizedException;
import com.example.URLShortener.repository.ClickCountryStatsRepository;
import com.example.URLShortener.repository.ClickHourlyRepository;
import com.example.URLShortener.repository.LinkStatsRepository;
import com.example.URLShortener.repository.ShortUrlRepository;
import com.example.URLShortener.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock ShortUrlRepository shortUrlRepository;
    @Mock LinkStatsRepository linkStatsRepository;
    @Mock ClickHourlyRepository clickHourlyRepository;
    @Mock ClickCountryStatsRepository clickCountryStatsRepository;
    @Mock CurrentUser currentUser;

    private AnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsServiceImpl(shortUrlRepository, linkStatsRepository,
                clickHourlyRepository, clickCountryStatsRepository, currentUser);
    }

    @Test
    void getAccountOverview_shouldCountStatusesAndSumClicks() {
        ShortUrl active = url(1L, ShortUrlStatus.ACTIVE, null);
        ShortUrl expired = url(2L, ShortUrlStatus.ACTIVE, LocalDateTime.now().minusDays(1));
        ShortUrl disabled = url(3L, ShortUrlStatus.DISABLED, null);
        ShortUrl deleted = url(4L, ShortUrlStatus.DELETED, null);

        when(currentUser.requireUserId()).thenReturn(7L);
        when(shortUrlRepository.findAllByOwnerId(7L))
                .thenReturn(List.of(active, expired, disabled, deleted));
        when(linkStatsRepository.findByShortUrlIdIn(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(List.of(
                        LinkStats.of(1L, 10L, Instant.now()),
                        LinkStats.of(3L, 5L, Instant.now())));

        AnalyticsOverviewResponse result = service.getAccountOverview();

        assertThat(result.getTotalLinks()).isEqualTo(4);
        assertThat(result.getTotalClicks()).isEqualTo(15);
        assertThat(result.getActiveLinks()).isEqualTo(1);
        assertThat(result.getExpiredLinks()).isEqualTo(1);
        assertThat(result.getDisabledLinks()).isEqualTo(1);
        assertThat(result.getDeletedLinks()).isEqualTo(1);
    }

    @Test
    void getAccountOverview_whenNoLinks_shouldReturnZeroes() {
        when(currentUser.requireUserId()).thenReturn(7L);
        when(shortUrlRepository.findAllByOwnerId(7L)).thenReturn(List.of());

        AnalyticsOverviewResponse result = service.getAccountOverview();

        assertThat(result.getTotalLinks()).isZero();
        assertThat(result.getTotalClicks()).isZero();
    }

    @Test
    void getAccountClickTimeseries_shouldGroupHourlyBucketsByDay() {
        LocalDate from = LocalDate.of(2026, 9, 20);
        LocalDate to = LocalDate.of(2026, 9, 22);

        when(currentUser.requireUserId()).thenReturn(7L);
        when(shortUrlRepository.findAllByOwnerId(7L)).thenReturn(List.of(url(1L, ShortUrlStatus.ACTIVE, null)));
        when(clickHourlyRepository.findByShortUrlIdsAndHourBetween(eq(List.of(1L)), any(), any()))
                .thenReturn(List.of(
                        hourly(1L, "2026-09-20T10:00:00Z", 3L),
                        hourly(1L, "2026-09-20T11:00:00Z", 2L),
                        hourly(1L, "2026-09-21T08:00:00Z", 7L)));

        ClickTimeseriesResponse result = service.getAccountClickTimeseries(from, to, "day");

        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().get(0).getDate()).isEqualTo("2026-09-20");
        assertThat(result.getItems().get(0).getClicks()).isEqualTo(5);
        assertThat(result.getItems().get(1).getDate()).isEqualTo("2026-09-21");
        assertThat(result.getItems().get(1).getClicks()).isEqualTo(7);
    }

    @Test
    void getAccountClickTimeseries_whenNoLinks_shouldReturnEmpty() {
        when(currentUser.requireUserId()).thenReturn(7L);
        when(shortUrlRepository.findAllByOwnerId(7L)).thenReturn(List.of());

        ClickTimeseriesResponse result = service.getAccountClickTimeseries(
                LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 22), "day");

        assertThat(result.getItems()).isEmpty();
    }

    // ─────────────────────────── getSummary ───────────────────────────

    @Test
    void getSummary_whenOwnedAndStatsExist_shouldReturnTotals() {
        Instant lastClick = Instant.parse("2026-09-24T10:00:00Z");
        when(currentUser.requireUserId()).thenReturn(7L);
        when(shortUrlRepository.findById(1L)).thenReturn(Optional.of(url(1L, ShortUrlStatus.ACTIVE, null)));
        when(linkStatsRepository.findByShortUrlId(1L))
                .thenReturn(Optional.of(LinkStats.of(1L, 42L, lastClick)));

        LinkSummaryResponse result = service.getSummary(1L);

        assertThat(result.getShortUrlId()).isEqualTo(1L);
        assertThat(result.getShortCode()).isEqualTo("code1");
        assertThat(result.getTotalClicks()).isEqualTo(42L);
        assertThat(result.getLastClickAt()).isEqualTo(lastClick);
    }

    @Test
    void getSummary_whenStatsMissing_shouldReturnZeroClicks() {
        when(currentUser.requireUserId()).thenReturn(7L);
        when(shortUrlRepository.findById(1L)).thenReturn(Optional.of(url(1L, ShortUrlStatus.ACTIVE, null)));
        when(linkStatsRepository.findByShortUrlId(1L)).thenReturn(Optional.empty());

        LinkSummaryResponse result = service.getSummary(1L);

        assertThat(result.getTotalClicks()).isZero();
        assertThat(result.getLastClickAt()).isNull();
    }

    @Test
    void getSummary_whenUrlMissing_shouldThrowResourceNotFound() {
        when(currentUser.requireUserId()).thenReturn(7L);
        when(shortUrlRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void getSummary_whenNotOwner_shouldThrowUnauthorized() {
        ShortUrl other = url(1L, ShortUrlStatus.ACTIVE, null);
        other.setOwnerId(999L); // owned by someone else
        when(currentUser.requireUserId()).thenReturn(7L);
        when(shortUrlRepository.findById(1L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.getSummary(1L))
                .isInstanceOf(UnauthorizedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    // ─────────────────────────── getHourlyClicks ───────────────────────────

    @Test
    void getHourlyClicks_shouldReturnBucketsInWindow() {
        when(currentUser.requireUserId()).thenReturn(7L);
        when(shortUrlRepository.findById(1L)).thenReturn(Optional.of(url(1L, ShortUrlStatus.ACTIVE, null)));
        when(clickHourlyRepository.findByShortUrlIdAndHourBetween(eq(1L), any(), any()))
                .thenReturn(List.of(
                        hourly(1L, "2026-09-24T09:00:00Z", 3L),
                        hourly(1L, "2026-09-24T10:00:00Z", 5L)));

        HourlyClicksResponse result = service.getHourlyClicks(1L, 24);

        assertThat(result.getShortUrlId()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().get(0).getHour()).isEqualTo("2026-09-24T09:00:00Z");
        assertThat(result.getItems().get(0).getClicks()).isEqualTo(3L);
        assertThat(result.getItems().get(1).getClicks()).isEqualTo(5L);
    }

    @Test
    void getHourlyClicks_whenHoursNonPositive_shouldDefaultTo24HourWindow() {
        when(currentUser.requireUserId()).thenReturn(7L);
        when(shortUrlRepository.findById(1L)).thenReturn(Optional.of(url(1L, ShortUrlStatus.ACTIVE, null)));
        when(clickHourlyRepository.findByShortUrlIdAndHourBetween(eq(1L), any(), any()))
                .thenReturn(List.of());

        service.getHourlyClicks(1L, 0);

        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(clickHourlyRepository).findByShortUrlIdAndHourBetween(eq(1L), fromCaptor.capture(), toCaptor.capture());
        long hoursBetween = java.time.Duration.between(fromCaptor.getValue(), toCaptor.getValue()).toHours();
        assertThat(hoursBetween).isEqualTo(24);
    }

    @Test
    void getHourlyClicks_whenNotOwner_shouldThrowUnauthorized() {
        ShortUrl other = url(1L, ShortUrlStatus.ACTIVE, null);
        other.setOwnerId(999L);
        when(currentUser.requireUserId()).thenReturn(7L);
        when(shortUrlRepository.findById(1L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.getHourlyClicks(1L, 24))
                .isInstanceOf(UnauthorizedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        verify(clickHourlyRepository, never()).findByShortUrlIdAndHourBetween(any(), any(), any());
    }

    // ─────────────────────────── getClicksByCountry ───────────────────────────

    @Test
    void getClicksByCountry_shouldAggregateAndSortDescending() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(currentUser.requireUserId()).thenReturn(7L);
        when(shortUrlRepository.findById(1L)).thenReturn(Optional.of(url(1L, ShortUrlStatus.ACTIVE, null)));
        when(clickCountryStatsRepository.findByShortUrlIdAndDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of(
                        countryStat(1L, today.minusDays(2), "VN", 3L),
                        countryStat(1L, today.minusDays(1), "VN", 2L),
                        countryStat(1L, today, "US", 10L)));

        CountryClicksResponse result = service.getClicksByCountry(1L, 30);

        assertThat(result.getShortUrlId()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(2);
        // US (10) before VN (3+2), sorted descending by clicks.
        assertThat(result.getItems().get(0).getCountry()).isEqualTo("US");
        assertThat(result.getItems().get(0).getClicks()).isEqualTo(10L);
        assertThat(result.getItems().get(1).getCountry()).isEqualTo("VN");
        assertThat(result.getItems().get(1).getClicks()).isEqualTo(5L);
    }

    @Test
    void getClicksByCountry_whenDaysNonPositive_shouldDefaultTo30DayWindow() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(currentUser.requireUserId()).thenReturn(7L);
        when(shortUrlRepository.findById(1L)).thenReturn(Optional.of(url(1L, ShortUrlStatus.ACTIVE, null)));
        when(clickCountryStatsRepository.findByShortUrlIdAndDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of());

        service.getClicksByCountry(1L, 0);

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(clickCountryStatsRepository)
                .findByShortUrlIdAndDateBetween(eq(1L), fromCaptor.capture(), eq(today));
        assertThat(fromCaptor.getValue()).isEqualTo(today.minusDays(30));
    }

    @Test
    void getClicksByCountry_whenNotOwner_shouldThrowUnauthorized() {
        ShortUrl other = url(1L, ShortUrlStatus.ACTIVE, null);
        other.setOwnerId(999L);
        when(currentUser.requireUserId()).thenReturn(7L);
        when(shortUrlRepository.findById(1L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.getClicksByCountry(1L, 30))
                .isInstanceOf(UnauthorizedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        verify(clickCountryStatsRepository, never()).findByShortUrlIdAndDateBetween(any(), any(), any());
    }

    private static ShortUrl url(Long id, ShortUrlStatus status, LocalDateTime expiresAt) {
        return ShortUrl.builder().id(id).shortCode("code" + id).originUrl("https://example.com")
                .ownerId(7L).status(status).expiresAt(expiresAt)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private static ClickHourly hourly(Long shortUrlId, String hourIso, Long count) {
        Instant hour = Instant.parse(hourIso);
        return ClickHourly.of(shortUrlId, hour, count);
    }

    private static ClickCountryStats countryStat(Long shortUrlId, LocalDate date, String country, Long clicks) {
        return ClickCountryStats.of(shortUrlId, date, country, clicks);
    }
}
