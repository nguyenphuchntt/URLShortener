package com.example.URLShortener.service.impl;

import com.example.URLShortener.dto.response.AnalyticsOverviewResponse;
import com.example.URLShortener.dto.response.ClickTimeseriesResponse;
import com.example.URLShortener.entity.ClickHourly;
import com.example.URLShortener.entity.LinkStats;
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.entity.enums.ShortUrlStatus;
import com.example.URLShortener.repository.ClickCountryStatsRepository;
import com.example.URLShortener.repository.ClickHourlyRepository;
import com.example.URLShortener.repository.LinkStatsRepository;
import com.example.URLShortener.repository.ShortUrlRepository;
import com.example.URLShortener.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private static ShortUrl url(Long id, ShortUrlStatus status, LocalDateTime expiresAt) {
        return ShortUrl.builder().id(id).shortCode("code" + id).originUrl("https://example.com")
                .ownerId(7L).status(status).expiresAt(expiresAt)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private static ClickHourly hourly(Long shortUrlId, String hourIso, Long count) {
        Instant hour = Instant.parse(hourIso);
        return ClickHourly.of(shortUrlId, hour, count);
    }
}
