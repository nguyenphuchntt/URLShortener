package com.example.URLShortener.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ratelimit")
@Getter
public class RateLimitProperties {

    private final boolean enabled = true;

    @Setter
    private LimitConfig defaultLimit = new LimitConfig(100, 60);

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LimitConfig {
        private int capacity;
        private int durationSeconds;
    }
}
