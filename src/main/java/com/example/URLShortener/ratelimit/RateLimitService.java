package com.example.URLShortener.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RateLimitService {

    private final ProxyManager<String> proxyManager;
    private final BucketConfigurationFactory configurationFactory;
    private final RateLimitProperties properties;

    public RateLimitResult tryConsume(
            String identifier,
            String endpointType) {
        String key = RateLimitKey.of(identifier, endpointType);
        RateLimitProperties.LimitConfig config = getLimitConfig(endpointType);
        Bucket bucket = proxyManager.builder()
                .build(key, configurationFactory.create(config));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return new RateLimitResult(true, probe.getRemainingTokens(), 0);
        }
        long retryAfterSeconds = Math.max(
                1,
                (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0)
        );
        return new RateLimitResult(false, 0, retryAfterSeconds);
    }

    private RateLimitProperties.LimitConfig getLimitConfig (String endpointType) {
        if (endpointType == null) {
            return properties.getDefaultLimit();
        }
        return switch (endpointType) {
            default -> properties.getDefaultLimit();
        };
    }
}
