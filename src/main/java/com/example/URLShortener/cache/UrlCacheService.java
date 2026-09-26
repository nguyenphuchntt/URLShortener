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

    public record CachedUrl (String originUrl, Instant expiresAt, Long shortUrlId) {
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

    public void put(String shortCode, String originUrl, Instant expiresAt, Long shortUrlId) {
        String value = encode(originUrl, expiresAt, shortUrlId);
        Duration ttl = ttlFor(expiresAt);
        try {
            redis.opsForValue().set(key(shortCode), value, ttl);
        } catch (Exception e) {
            log.warn("Redis is temporary fail due to: ", e.getCause());
        }
    }

    public void put(String shortCode, String originUrl, Instant expiresAt) {
        put(shortCode, originUrl, expiresAt, null);
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
     * Revert the encoded value stored in cache -> originUrl|expiresAt(millis)|shortUrlId
     *
     * <p>The current format always has three fields, so the trailing pair is taken from the
     * right (the origin URL itself may contain '|'). Legacy two-field entries written by an
     * older version have no shortUrlId and decode with a null id.
     */
    private CachedUrl decode(String raw) {
        String[] parts = raw.split("\\|", -1);
        if (parts.length == 1) {
            return new CachedUrl(raw, null, null);
        }
        if (parts.length == 2) {
            return new CachedUrl(parts[0], parseExpiry(parts[1]), null);
        }
        Long shortUrlId = parts[parts.length - 1].isEmpty() ? null : Long.parseLong(parts[parts.length - 1]);
        Instant expiresAt = parseExpiry(parts[parts.length - 2]);
        String url = String.join("|", java.util.Arrays.copyOfRange(parts, 0, parts.length - 2));
        return new CachedUrl(url, expiresAt, shortUrlId);
    }

    private static Instant parseExpiry(String raw) {
        return raw.isEmpty() || "0".equals(raw) ? null : Instant.ofEpochMilli(Long.parseLong(raw));
    }

    private String encode(String originUrl, Instant expiresAt, Long shortUrlId) {
        long millis = expiresAt == null ? 0L : expiresAt.toEpochMilli();
        return originUrl + "|" + millis + "|" + (shortUrlId == null ? "" : shortUrlId);
    }
}
