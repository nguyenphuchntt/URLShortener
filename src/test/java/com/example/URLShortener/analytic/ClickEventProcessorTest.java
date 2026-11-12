package com.example.URLShortener.analytic;

import com.example.URLShortener.entity.ClickEvent;
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.entity.enums.ShortUrlStatus;
import com.example.URLShortener.repository.ClickEventRepository;
import com.example.URLShortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClickEventProcessorTest {

    @Mock ClickEventRepository clickEventRepository;
    @Mock ShortUrlRepository shortUrlRepository;
    @Mock GeoIpService geoIpService;

    private ClickEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ClickEventProcessor(clickEventRepository, shortUrlRepository, geoIpService);
    }

    @Test
    void process_whenGeoKnown_shouldPersistAndWriteAllRollups() {
        ClickEventPayload p = payload(1L, "8.8.8.8", 1_700_000_000_000L);
        givenShortUrlExists(1L);
        when(geoIpService.resolve("8.8.8.8")).thenReturn(
                new GeoIpService.GeoLocation("VN", "Hanoi"));

        processor.process(p);

        ArgumentCaptor<ClickEvent> eventCaptor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(clickEventRepository).save(eventCaptor.capture());
        ClickEvent saved = eventCaptor.getValue();
        assertThat(saved.getShortCode()).isEqualTo("code1");
        assertThat(saved.getIpAddress()).isEqualTo("8.8.8.8");
        assertThat(saved.getCountry()).isEqualTo("VN");
        assertThat(saved.getCity()).isEqualTo("Hanoi");

        // 1_700_000_000_000 ms = 2023-11-14T22:13:20Z; buckets are UTC regardless of JVM zone
        assertThat(saved.getClickedAt()).isEqualTo(Instant.ofEpochMilli(1_700_000_000_000L));
        verify(clickEventRepository).upsertHourly(1L, Instant.parse("2023-11-14T22:00:00Z"));
        verify(clickEventRepository).upsertLinkStats(1L, Instant.ofEpochMilli(1_700_000_000_000L));
        verify(clickEventRepository).upsertCountryStats(1L, LocalDate.of(2023, 11, 14), "VN");
    }

    @Test
    void process_whenGeoUnknown_shouldSkipCountryStatOnly() {
        ClickEventPayload p = payload(1L, "10.0.0.1", 1_700_000_000_000L);
        givenShortUrlExists(1L);
        when(geoIpService.resolve("10.0.0.1")).thenReturn(GeoIpService.GeoLocation.UNKNOWN);

        processor.process(p);

        verify(clickEventRepository).save(any(ClickEvent.class));
        verify(clickEventRepository).upsertHourly(eq(1L), any(Instant.class));
        verify(clickEventRepository).upsertLinkStats(eq(1L), any(Instant.class));
        verify(clickEventRepository, never()).upsertCountryStats(anyLong(), any(), anyString());
    }

    @Test
    void process_whenShortUrlIdMissing_shouldSkipEntirely() {
        ClickEventPayload p = payload(null, "8.8.8.8", 1_700_000_000_000L);

        processor.process(p);

        verify(clickEventRepository, never()).save(any());
        verify(shortUrlRepository, never()).findById(anyLong());
        verify(geoIpService, never()).resolve(any());
    }

    @Test
    void process_whenShortUrlUnknown_shouldSkipEntirely() {
        ClickEventPayload p = payload(1L, "8.8.8.8", 1_700_000_000_000L);
        when(shortUrlRepository.findById(1L)).thenReturn(Optional.empty());

        processor.process(p);

        verify(clickEventRepository, never()).save(any());
        verify(geoIpService, never()).resolve(any());
    }

    @Test
    void process_whenTimestampMissing_shouldUseCurrentTimeAndStillPersist() {
        ClickEventPayload p = payload(1L, "8.8.8.8", null);
        givenShortUrlExists(1L);
        when(geoIpService.resolve("8.8.8.8")).thenReturn(GeoIpService.GeoLocation.UNKNOWN);

        processor.process(p);

        ArgumentCaptor<ClickEvent> eventCaptor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(clickEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getClickedAt()).isNotNull();
    }

    // ─────────────────────────── helpers ───────────────────────────

    private void givenShortUrlExists(Long shortUrlId) {
        when(shortUrlRepository.findById(shortUrlId)).thenReturn(Optional.of(
                ShortUrl.builder()
                        .id(shortUrlId)
                        .shortCode("code" + shortUrlId)
                        .originUrl("https://example.com")
                        .ownerId(7L)
                        .status(ShortUrlStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    private static ClickEventPayload payload(Long shortUrlId, String ip, Long timestamp) {
        return ClickEventPayload.builder()
                .shortUrlId(shortUrlId)
                .ip(ip)
                .userAgent("k6")
                .referrer("https://ref.example.com")
                .timestamp(timestamp)
                .build();
    }
}
