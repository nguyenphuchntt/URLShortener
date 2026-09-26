package com.example.URLShortener.analytic;

import com.example.URLShortener.entity.ClickEvent;
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.repository.ClickEventRepository;
import com.example.URLShortener.repository.ShortUrlRepository;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClickEventConsumer
        implements StreamListener<String, ObjectRecord<String, String>> {
    //                            stream_id           stream_id  value_json
    private final ClickEventRepository clickEventRepository;
    private final ShortUrlRepository shortUrlRepository;
    private final GeoIpService geoIpService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void onMessage(ObjectRecord<String, String> message) {
        try {
            String json = message.getValue();
            ClickEventPayload payload =
                    objectMapper.readValue(json, ClickEventPayload.class);
            process(payload);
        } catch (Exception e) {
            log.error(
                    "Failed to process click event, messageId={}, will NOT retry (ack to prevent poison message)",
                    message.getId(),
                    e
            );
        }
    }

    private void process(ClickEventPayload payload) {
        Long shortUrlId = payload.getShortUrlId();
        String ip = payload.getIp();

        if (shortUrlId == null) {
            log.warn("Skipping click event with missing shortUrlId");
            return;
        }
        // short_code
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
        LocalDateTime clickedAtLocal = LocalDateTime.ofInstant(clickedAt, ZoneOffset.UTC);
        // country/ city
        GeoIpService.GeoLocation geo = geoIpService.resolve(ip);
        String country = geo.isKnown() ? geo.country() : null;
        String city = geo.isKnown() ? geo.city() : null;

        // save raw log
        ClickEvent event = ClickEvent.builder()
                .shortCode(shortCode)
                .ipAddress(ip)
                .userAgent(payload.getUserAgent())
                .referrer(payload.getReferrer())
                .clickedAt(clickedAt)
                .country(country)
                .city(city)
                .build();
        clickEventRepository.save(event);

        // hourly rollup
        clickEventRepository.upsertHourly(shortUrlId, clickedAtLocal);

        // lifetime totals
        clickEventRepository.upsertLinkStats(shortUrlId, clickedAtLocal);

        // country stats
        if (geo.isKnown()) {
            LocalDate statDate = clickedAtLocal.toLocalDate();
            clickEventRepository.upsertCountryStats(
                    shortUrlId,
                    statDate.atStartOfDay(),
                    geo.country());
        } else {
            log.debug("Geo lookup unavailable for ip={}, skipping country stat", ip);
        }

        log.info("Click event processed for shortUrlId={}, shortCode={}", shortUrlId, shortCode);
    }
}
