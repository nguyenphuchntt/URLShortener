package com.example.URLShortener.ratelimit;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RateLimitKey {
    private static final String PREFIX = "ratelimit:";

    private final String identifier;
    private final String endpointType;

    public static String of(String identifier, String endpointType) {
        return identifier +
                ":" +
                endpointType;
    }
}
