package com.example.URLShortener.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlCacheServiceTest {

    private static final String KEY = "urlshortener:url:abc123";
    private static final String ORIGIN = "https://example.com";

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    private UrlCacheService service;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        service = new UrlCacheService(redis);
    }

    @Test
    void get_whenKeyMissing_shouldReturnEmpty() {
        when(valueOps.get(KEY)).thenReturn(null);

        assertThat(service.get("abc123")).isEmpty();
    }

    @Test
    void get_whenRedisFails_shouldReturnEmpty() {
        when(valueOps.get(KEY)).thenThrow(new RuntimeException("redis down"));

        assertThat(service.get("abc123")).isEmpty();
    }

    @Test
    void get_whenValueHasExpiry_shouldDecodeUrlAndExpiry() {
        Instant expiresAt = Instant.parse("2026-09-23T10:00:00Z");
        when(valueOps.get(KEY)).thenReturn(ORIGIN + "|" + expiresAt.toEpochMilli());

        Optional<UrlCacheService.CachedUrl> cached = service.get("abc123");

        assertThat(cached).isPresent();
        assertThat(cached.get().originUrl()).isEqualTo(ORIGIN);
        assertThat(cached.get().expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void get_whenValueHasNoSeparator_shouldDecodeWithoutExpiry() {
        when(valueOps.get(KEY)).thenReturn(ORIGIN);

        assertThat(service.get("abc123")).get()
                .satisfies(cached -> {
                    assertThat(cached.originUrl()).isEqualTo(ORIGIN);
                    assertThat(cached.expiresAt()).isNull();
                });
    }

    @Test
    void get_whenMillisIsZero_shouldDecodeExpiryAsNull() {
        when(valueOps.get(KEY)).thenReturn(ORIGIN + "|0|");

        assertThat(service.get("abc123")).get()
                .extracting(UrlCacheService.CachedUrl::expiresAt)
                .isNull();
    }

    @Test
    void get_whenValueHasShortUrlId_shouldDecodeIt() {
        Instant expiresAt = Instant.parse("2026-09-23T10:00:00Z");
        when(valueOps.get(KEY)).thenReturn(ORIGIN + "|" + expiresAt.toEpochMilli() + "|42");

        Optional<UrlCacheService.CachedUrl> cached = service.get("abc123");

        assertThat(cached).isPresent();
        assertThat(cached.get().originUrl()).isEqualTo(ORIGIN);
        assertThat(cached.get().expiresAt()).isEqualTo(expiresAt);
        assertThat(cached.get().shortUrlId()).isEqualTo(42L);
    }

    @Test
    void get_whenLegacyValueHasNoShortUrlId_shouldDecodeWithNullId() {
        Instant expiresAt = Instant.parse("2026-09-23T10:00:00Z");
        when(valueOps.get(KEY)).thenReturn(ORIGIN + "|" + expiresAt.toEpochMilli());

        Optional<UrlCacheService.CachedUrl> cached = service.get("abc123");

        assertThat(cached).isPresent();
        assertThat(cached.get().originUrl()).isEqualTo(ORIGIN);
        assertThat(cached.get().expiresAt()).isEqualTo(expiresAt);
        assertThat(cached.get().shortUrlId()).isNull();
    }

    @Test
    void get_whenUrlContainsSeparator_shouldKeepUrlIntact() {
        when(valueOps.get(KEY)).thenReturn("https://example.com/a|b|1700000000000|7");

        Optional<UrlCacheService.CachedUrl> cached = service.get("abc123");

        assertThat(cached).get()
                .extracting(UrlCacheService.CachedUrl::originUrl)
                .isEqualTo("https://example.com/a|b");
        assertThat(cached.get().shortUrlId()).isEqualTo(7L);
    }

    @Test
    void cachedUrl_whenExpiryInPast_shouldBeExpired() {
        assertThat(new UrlCacheService.CachedUrl(ORIGIN, Instant.now().minusSeconds(1), null).isExpired()).isTrue();
    }

    @Test
    void cachedUrl_whenExpiryInFuture_shouldNotBeExpired() {
        assertThat(new UrlCacheService.CachedUrl(ORIGIN, Instant.now().plusSeconds(60), null).isExpired()).isFalse();
    }

    @Test
    void cachedUrl_whenExpiryIsNull_shouldNotBeExpired() {
        assertThat(new UrlCacheService.CachedUrl(ORIGIN, null, null).isExpired()).isFalse();
    }

    @Test
    void put_whenNoExpiry_shouldEncodeZeroAndUseMaxTtlOfOneHour() {
        service.put("abc123", ORIGIN, null);

        verify(valueOps).set(KEY, ORIGIN + "|0|", Duration.ofHours(1));
    }

    @Test
    void put_withShortUrlId_shouldEncodeIt() {
        service.put("abc123", ORIGIN, null, 99L);

        verify(valueOps).set(KEY, ORIGIN + "|0|99", Duration.ofHours(1));
    }

    @Test
    void put_whenExpirySoon_shouldUseRemainingTtl() {
        service.put("abc123", ORIGIN, Instant.now().plus(Duration.ofMinutes(5)));

        assertThat(capturedTtl()).isBetween(Duration.ofMinutes(4), Duration.ofMinutes(5));
    }

    @Test
    void put_whenExpiryBeyondMax_shouldCapTtlAtOneHour() {
        service.put("abc123", ORIGIN, Instant.now().plus(Duration.ofDays(3)));

        assertThat(capturedTtl()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void put_whenAlreadyExpired_shouldUseMinimumTtlOfOneSecond() {
        service.put("abc123", ORIGIN, Instant.now().minus(Duration.ofMinutes(1)));

        assertThat(capturedTtl()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void put_whenRedisFails_shouldNotPropagate() {
        doThrow(new RuntimeException("redis down"))
                .when(valueOps).set(eq(KEY), anyString(), any(Duration.class));

        assertThatCode(() -> service.put("abc123", ORIGIN, null)).doesNotThrowAnyException();
    }

    @Test
    void evict_shouldDeletePrefixedKey() {
        service.evict("abc123");

        verify(redis).delete(KEY);
    }

    @Test
    void evict_whenRedisFails_shouldNotPropagate() {
        when(redis.delete(KEY)).thenThrow(new RuntimeException("redis down"));

        assertThatCode(() -> service.evict("abc123")).doesNotThrowAnyException();
    }

    private Duration capturedTtl() {
        ArgumentCaptor<Duration> captor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(eq(KEY), anyString(), captor.capture());
        return captor.getValue();
    }
}
