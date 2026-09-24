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
import com.example.URLShortener.entity.enums.ShortUrlStatus;
import com.example.URLShortener.exception.ResourceNotFoundException;
import com.example.URLShortener.exception.UnauthorizedException;
import com.example.URLShortener.repository.ClickCountryStatsRepository;
import com.example.URLShortener.repository.ClickHourlyRepository;
import com.example.URLShortener.repository.LinkStatsRepository;
import com.example.URLShortener.repository.ShortUrlRepository;
import com.example.URLShortener.security.CurrentUser;
import com.example.URLShortener.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ShortUrlRepository shortUrlRepository;
    private final LinkStatsRepository linkStatsRepository;
    private final ClickHourlyRepository clickHourlyRepository;
    private final ClickCountryStatsRepository clickCountryStatsRepository;
    private final CurrentUser currentUser;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse getAccountOverview() {
        Long userId = currentUser.requireUserId();
        List<ShortUrl> urls = shortUrlRepository.findAllByOwnerId(userId);
        List<Long> urlIds = urls.stream().map(ShortUrl::getId).collect(Collectors.toList());

        long totalClicks = urlIds.isEmpty()
                ? 0L
                : linkStatsRepository.findByShortUrlIdIn(urlIds).stream()
                        .mapToLong(LinkStats::getTotalClicks)
                        .sum();

        LocalDateTime now = LocalDateTime.now();
        long activeLinks = 0, disabledLinks = 0, deletedLinks = 0, expiredLinks = 0;
        for (ShortUrl url : urls) {
            if (url.getStatus() == ShortUrlStatus.DELETED) {
                deletedLinks++;
            } else if (url.getStatus() == ShortUrlStatus.DISABLED) {
                disabledLinks++;
            } else if (url.getStatus() == ShortUrlStatus.ACTIVE) {
                if (url.getExpiresAt() != null && !url.getExpiresAt().isAfter(now)) {
                    expiredLinks++;
                } else {
                    activeLinks++;
                }
            }
        }

        return AnalyticsOverviewResponse.builder()
                .totalLinks(urls.size())
                .totalClicks(totalClicks)
                .activeLinks(activeLinks)
                .disabledLinks(disabledLinks)
                .expiredLinks(expiredLinks)
                .deletedLinks(deletedLinks)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ClickTimeseriesResponse getAccountClickTimeseries(LocalDate from, LocalDate to, String granularity) {
        Long userId = currentUser.requireUserId();
        List<Long> urlIds = shortUrlRepository.findAllByOwnerId(userId).stream()
                .map(ShortUrl::getId)
                .collect(Collectors.toList());

        if (urlIds.isEmpty()) {
            return ClickTimeseriesResponse.builder().items(List.of()).build();
        }

        Instant fromHour = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toHour = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Aggregate hourly buckets into days (frontend always sends granularity=day).
        Map<LocalDate, Long> byDay = clickHourlyRepository
                .findByShortUrlIdsAndHourBetween(urlIds, fromHour, toHour)
                .stream()
                .collect(Collectors.groupingBy(
                        ch -> ch.getId().getHour().atZone(ZoneOffset.UTC).toLocalDate(),
                        Collectors.summingLong(ClickHourly::getClickCount)));

        List<ClickTimeseriesResponse.Item> items = byDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> ClickTimeseriesResponse.Item.builder()
                        .date(e.getKey().toString())
                        .clicks(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return ClickTimeseriesResponse.builder().items(items).build();
    }

    @Override
    @Transactional(readOnly = true)
    public LinkSummaryResponse getSummary(Long shortUrlId) {
        ShortUrl url = requireOwnedUrl(shortUrlId);
        LinkStats stats = linkStatsRepository.findByShortUrlId(shortUrlId).orElse(null);
        return LinkSummaryResponse.builder()
                .shortUrlId(shortUrlId)
                .shortCode(url.getShortCode())
                .totalClicks(stats != null ? stats.getTotalClicks() : 0L)
                .lastClickAt(stats != null ? stats.getLastClickAt() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public HourlyClicksResponse getHourlyClicks(Long shortUrlId, int hours) {
        requireOwnedUrl(shortUrlId);
        int window = hours > 0 ? hours : 24;
        Instant to = Instant.now();
        Instant from = to.minus(window, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);

        List<HourlyClicksResponse.Item> items = clickHourlyRepository
                .findByShortUrlIdAndHourBetween(shortUrlId, from, to)
                .stream()
                .map(ch -> HourlyClicksResponse.Item.builder()
                        .hour(ch.getId().getHour().toString())
                        .clicks(ch.getClickCount())
                        .build())
                .collect(Collectors.toList());

        return HourlyClicksResponse.builder().shortUrlId(shortUrlId).items(items).build();
    }

    @Override
    @Transactional(readOnly = true)
    public CountryClicksResponse getClicksByCountry(Long shortUrlId, int days) {
        requireOwnedUrl(shortUrlId);
        int window = days > 0 ? days : 30;
        LocalDate to = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = to.minusDays(window);

        // aggregate per country across the requested date range
        List<ClickCountryStats> rows = clickCountryStatsRepository
                .findByShortUrlIdAndDateBetween(shortUrlId, from, to);

        List<CountryClicksResponse.Item> items = rows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getId().getCountry(),
                        Collectors.summingLong(ClickCountryStats::getClicks)))
                .entrySet().stream()
                .map(e -> CountryClicksResponse.Item.builder()
                        .country(e.getKey())
                        .clicks(e.getValue())
                        .build())
                .sorted((a, b) -> Long.compare(b.getClicks(), a.getClicks()))
                .collect(Collectors.toList());

        return CountryClicksResponse.builder().shortUrlId(shortUrlId).items(items).build();
    }

    private ShortUrl requireOwnedUrl(Long shortUrlId) {
        Long userId = currentUser.requireUserId();
        ShortUrl url = shortUrlRepository.findById(shortUrlId)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        if (!url.getOwnerId().equals(userId)) {
            throw new UnauthorizedException("Not owner of this short URL");
        }
        return url;
    }
}
