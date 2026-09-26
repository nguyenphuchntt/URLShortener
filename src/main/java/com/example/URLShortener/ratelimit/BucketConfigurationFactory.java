package com.example.URLShortener.ratelimit;

import io.github.bucket4j.BucketConfiguration;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class BucketConfigurationFactory {

    public BucketConfiguration create(
            RateLimitProperties.LimitConfig config) {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit
                        .capacity(config.getCapacity())
                        .refillGreedy(config.getCapacity(),
                                Duration.ofSeconds(config.getDurationSeconds()))
                ).build();
    }
}
