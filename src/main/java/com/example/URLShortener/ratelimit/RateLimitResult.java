package com.example.URLShortener.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RateLimitResult {

    public final boolean allowed;
    public final long remainingTokens;
    public final long retryAfterSeconds;

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
