package com.example.URLShortener.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class UrlCacheService {

    private static final String PREFIX = "urlshortener:url:";
    private static final Duration MAX_TTL = Duration.ofHours(1);
    private final StringRedisTemplate redis;

    public UrlCacheService(
            StringRedisTemplate redis) {
        this.redis = redis;
    }

    public record CachedUrl (String originUrl, Instant expiresAt) {
        public boolean isExpired() {
            return expiresAt != null && expiresAt.isBefore(Instant.now());
        }
    }

    public Optional<CachedUrl> get(String shortCode) {
        String raw;
        try {
            raw = redis.opsForValue().get(key(shortCode));
        } catch (Exception e) {
            return Optional.empty();
        }
        return Optional.ofNullable(raw).map(this::decode);
    }

    public void put(String shortCode, String originUrl, Instant expiresAt) {
        String value = encode(originUrl, expiresAt);
        Duration ttl = ttlFor(expiresAt);
        try {
            redis.opsForValue().set(key(shortCode), value, ttl);
        } catch (Exception e) {
            log.warn("Redis is temporary fail due to: ", e.getCause());
        }
    }

    public void evict(String shortCode) {
        try {
            redis.delete(key(shortCode));
        } catch (Exception e) {
            log.warn("Redis is temporary fail due to: ", e.getCause());
        }
    }

    private Duration ttlFor(Instant expiresAt) {
        if (expiresAt == null) return MAX_TTL;
        Duration untilExpiry = Duration.between(Instant.now(), expiresAt);
        if (untilExpiry.isNegative() || untilExpiry.isZero()) {
            return Duration.ofSeconds(1);
        }
        return untilExpiry.compareTo(MAX_TTL) < 0 ? untilExpiry : MAX_TTL;
    }

    private String key(String shortCode) {
        return PREFIX + shortCode;
    }

    /**
     * Revert the encoded value stored in cache -> shortCode|expiresAt(millis)
     * @param raw
     * @return
     */
    private CachedUrl decode(String raw) {
        int sep = raw.lastIndexOf('|');
        if (sep < 0) {
            return new CachedUrl(raw, null);
        }
        String url = raw.substring(0, sep);
        long millis = Long.parseLong(raw.substring(sep + 1));
        Instant expiresAt = millis == 0L ? null : Instant.ofEpochMilli(millis);
        return new CachedUrl(url, expiresAt);
    }

    private String encode(String originUrl, Instant expiresAt) {
        long millis = expiresAt == null ? 0L : expiresAt.toEpochMilli();
        return originUrl + "|" + millis;
    }
}
