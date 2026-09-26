package com.example.URLShortener.ratelimit;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class Bucket4jConfig {

    @Bean
    public ProxyManager<String> proxyManager(RedisConnectionFactory connectionFactory) {
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) connectionFactory;

        RedisURI.Builder uriBuilder = RedisURI.builder()
                .withHost(lettuceFactory.getStandaloneConfiguration().getHostName())
                .withPort(lettuceFactory.getStandaloneConfiguration().getPort());
        // Password is optional: Redis may run without auth (local dev, test profile).
        // RedisPassword is empty then, and calling .get() on it throws NoSuchElementException.
        RedisPassword password = lettuceFactory.getStandaloneConfiguration().getPassword();
        if (password.isPresent()) {
            uriBuilder.withPassword(password.get());
        }

        RedisClient redisClient = RedisClient.create(uriBuilder.build());

        StatefulRedisConnection<String, byte[]> connection = redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );

        return LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                Duration.ofSeconds(120)
                        )
                )
                .build();
    }
}
