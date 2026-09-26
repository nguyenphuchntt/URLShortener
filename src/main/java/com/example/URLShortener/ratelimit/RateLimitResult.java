package com.example.URLShortener.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RateLimitResult {

    private final boolean allowed;
    private final long remainingTokens;
    private final long retryAfterSeconds;

    public static RateLimitResult allowed(long remainingTokens) {
        return new RateLimitResult(
                true,
                remainingTokens,
                0
        );
    }

    public static RateLimitResult rejected(long retryAfterSeconds) {
        return new RateLimitResult(
                false,
                0,
                retryAfterSeconds
        );
    }
}
