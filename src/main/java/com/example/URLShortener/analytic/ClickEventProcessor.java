package com.example.URLShortener.analytic;

import com.example.URLShortener.entity.ClickEvent;
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.repository.ClickEventRepository;
import com.example.URLShortener.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Persists one click: raw row + hourly, lifetime and country rollups, all in one transaction.
 * Kept separate from {@link ClickEventConsumer} so a DB failure rolls back cleanly and surfaces
 * as an exception to the consumer, which then leaves the stream entry pending for retry.
 * All time buckets are UTC.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClickEventProcessor {

    private final ClickEventRepository clickEventRepository;
    private final ShortUrlRepository shortUrlRepository;
    private final GeoIpService geoIpService;

    /**
     * Returns normally when the click was persisted or deliberately skipped (unknown link);
     * throws when persistence failed and the event should be retried.
     */
    @Transactional
    public void process(ClickEventPayload payload) {
        Long shortUrlId = payload.getShortUrlId();
        if (shortUrlId == null) {
            log.warn("Skipping click event with missing shortUrlId");
            return;
        }

        String shortCode = shortUrlRepository.findById(shortUrlId)
                .map(ShortUrl::getShortCode)
                .orElse(null);
        if (shortCode == null) {
            log.warn("Skipping click event for unknown shortUrlId={}", shortUrlId);
            return;
        }

        Instant clickedAt = payload.getTimestamp() != null
                ? Instant.ofEpochMilli(payload.getTimestamp())
                : Instant.now();

        String ip = payload.getIp();
        GeoIpService.GeoLocation geo = geoIpService.resolve(ip);
        String country = geo.isKnown() ? geo.country() : null;
        String city = geo.isKnown() ? geo.city() : null;

        clickEventRepository.save(ClickEvent.builder()
                .shortCode(shortCode)
                .ipAddress(ip)
                .userAgent(payload.getUserAgent())
                .referrer(payload.getReferrer())
                .clickedAt(clickedAt)
                .country(country)
                .city(city)
                .build());

        clickEventRepository.upsertHourly(shortUrlId, clickedAt.truncatedTo(ChronoUnit.HOURS));
        clickEventRepository.upsertLinkStats(shortUrlId, clickedAt);
        if (geo.isKnown()) {
            clickEventRepository.upsertCountryStats(
                    shortUrlId, LocalDate.ofInstant(clickedAt, ZoneOffset.UTC), geo.country());
        } else {
            log.debug("Geo lookup unavailable for ip={}, skipping country stat", ip);
        }

        log.debug("Click event processed for shortUrlId={}, shortCode={}", shortUrlId, shortCode);
    }
}
