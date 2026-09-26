package com.example.URLShortener.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    private static final String PREFIX = "urlshortener:jwt:blacklist:";

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    private TokenBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new TokenBlacklistService(redis);
    }

    @Test
    void revoke_shouldStoreJtiWithRemainingTtl() {
        when(redis.opsForValue()).thenReturn(valueOps);

        service.revoke("jti-1", 600L);

        verify(valueOps).set(PREFIX + "jti-1", "1", Duration.ofSeconds(600L));
    }

    @Test
    void revoke_whenTtlIsZero_shouldSkip() {
        service.revoke("jti-1", 0L);

        verify(redis, never()).opsForValue();
    }

    @Test
    void revoke_whenJtiBlank_shouldSkip() {
        service.revoke("  ", 600L);

        verify(redis, never()).opsForValue();
    }

    @Test
    void revoke_whenRedisFails_shouldNotPropagate() {
        when(redis.opsForValue()).thenThrow(new RuntimeException("redis down"));

        assertThatCode(() -> service.revoke("jti-1", 600L)).doesNotThrowAnyException();
    }

    @Test
    void isBlacklisted_whenKeyPresent_shouldReturnTrue() {
        when(redis.hasKey(PREFIX + "jti-1")).thenReturn(true);

        assertThat(service.isBlacklisted("jti-1")).isTrue();
    }

    @Test
    void isBlacklisted_whenKeyAbsent_shouldReturnFalse() {
        when(redis.hasKey(PREFIX + "jti-1")).thenReturn(false);

        assertThat(service.isBlacklisted("jti-1")).isFalse();
    }

    @Test
    void isBlacklisted_whenRedisFails_shouldFailClosed() {
        when(redis.hasKey(anyString())).thenThrow(new RuntimeException("redis down"));

        assertThat(service.isBlacklisted("jti-1")).isTrue();
    }

    @Test
    void isBlacklisted_whenJtiMissing_shouldFailClosed() {
        assertThat(service.isBlacklisted(null)).isTrue();
        verify(redis, never()).hasKey(any());
    }
}
