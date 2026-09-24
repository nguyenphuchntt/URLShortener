package com.example.URLShortener.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "urlshortener:jwt:blacklist:";

    private final StringRedisTemplate redis;

    public void revoke(String jti, long ttlSeconds) {
        if (jti == null || jti.isBlank() || ttlSeconds <= 0) {
            return;
        }
        try {
            redis.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
            log.debug("Revoked token jti={} for {}s", jti, ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to blacklist token jti={}", jti, e);
        }
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return true;
        }
        try {
            return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + jti));
        } catch (Exception e) {
            log.error("Failed to check token blacklist for jti={}, failing closed", jti, e);
            return true;
        }
    }
}
