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
import org.springframework.data.redis.connection.stream.ObjectRecord;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ClickEventConsumer là hộp đen 1 chiều: nhận JSON click từ Redis Stream, ghi raw log +
 * 3 rollup. Test này kiểm tra branching (geo có/không, shortUrlId thiếu, shortUrl không tồn
 * tại) và hành vi nuốt lỗi (poison message được ack, không rethrow).
 */
@ExtendWith(MockitoExtension.class)
class ClickEventConsumerTest {

    private static final String JSON = "{\"shortUrlId\":1}";

    @Mock ClickEventRepository clickEventRepository;
    @Mock ShortUrlRepository shortUrlRepository;
    @Mock GeoIpService geoIpService;
    @Mock ObjectMapper objectMapper;
    @Mock ObjectRecord<String, String> message;

    private ClickEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ClickEventConsumer(clickEventRepository, shortUrlRepository, geoIpService, objectMapper);
        lenient().when(message.getValue()).thenReturn(JSON);
    }

    @Test
    void onMessage_whenGeoKnown_shouldPersistAndWriteAllRollups() {
        givenPayload(payload(1L, "8.8.8.8", 1_700_000_000_000L));
        givenShortUrlExists(1L);
        when(geoIpService.resolve("8.8.8.8")).thenReturn(
                new GeoIpService.GeoLocation("VN", "Hanoi"));

        consumer.onMessage(message);

        ArgumentCaptor<ClickEvent> eventCaptor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(clickEventRepository).save(eventCaptor.capture());
        ClickEvent saved = eventCaptor.getValue();
        assertThat(saved.getShortCode()).isEqualTo("code1");
        assertThat(saved.getIpAddress()).isEqualTo("8.8.8.8");
        assertThat(saved.getCountry()).isEqualTo("VN");
        assertThat(saved.getCity()).isEqualTo("Hanoi");

        verify(clickEventRepository).upsertHourly(eq(1L), any(LocalDateTime.class));
        verify(clickEventRepository).upsertLinkStats(eq(1L), any(LocalDateTime.class));
        verify(clickEventRepository).upsertCountryStats(eq(1L), any(LocalDateTime.class), eq("VN"));
    }

    @Test
    void onMessage_whenGeoUnknown_shouldSkipCountryStatOnly() {
        givenPayload(payload(1L, "10.0.0.1", 1_700_000_000_000L));
        givenShortUrlExists(1L);
        when(geoIpService.resolve("10.0.0.1")).thenReturn(GeoIpService.GeoLocation.UNKNOWN);

        consumer.onMessage(message);

        verify(clickEventRepository).save(any(ClickEvent.class));
        verify(clickEventRepository).upsertHourly(eq(1L), any(LocalDateTime.class));
        verify(clickEventRepository).upsertLinkStats(eq(1L), any(LocalDateTime.class));
        verify(clickEventRepository, never()).upsertCountryStats(anyLong(), any(), anyString());
    }

    @Test
    void onMessage_whenShortUrlIdMissing_shouldSkipEntirely() {
        givenPayload(payload(null, "8.8.8.8", 1_700_000_000_000L));

        consumer.onMessage(message);

        verify(clickEventRepository, never()).save(any());
        verify(shortUrlRepository, never()).findById(anyLong());
        verify(geoIpService, never()).resolve(any());
    }

    @Test
    void onMessage_whenShortUrlUnknown_shouldSkipEntirely() {
        givenPayload(payload(1L, "8.8.8.8", 1_700_000_000_000L));
        when(shortUrlRepository.findById(1L)).thenReturn(Optional.empty());

        consumer.onMessage(message);

        verify(clickEventRepository, never()).save(any());
        verify(geoIpService, never()).resolve(any());
    }

    @Test
    void onMessage_whenTimestampMissing_shouldUseCurrentTimeAndStillPersist() {
        givenPayload(payload(1L, "8.8.8.8", null));
        givenShortUrlExists(1L);
        when(geoIpService.resolve("8.8.8.8")).thenReturn(GeoIpService.GeoLocation.UNKNOWN);

        consumer.onMessage(message);

        ArgumentCaptor<ClickEvent> eventCaptor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(clickEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getClickedAt()).isNotNull();
    }

    @Test
    void onMessage_whenPayloadMalformed_shouldSwallowSoMessageIsAcked() throws Exception {
        when(objectMapper.readValue(JSON, ClickEventPayload.class))
                .thenThrow(new RuntimeException("malformed json"));

        assertThatCode(() -> consumer.onMessage(message)).doesNotThrowAnyException();
        verify(clickEventRepository, never()).save(any());
    }

    // ─────────────────────────── helpers ───────────────────────────

    private void givenPayload(ClickEventPayload payload) {
        try {
            when(objectMapper.readValue(JSON, ClickEventPayload.class)).thenReturn(payload);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

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
