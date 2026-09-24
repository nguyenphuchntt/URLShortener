package com.example.URLShortener.config;

import com.example.URLShortener.analytic.ClickEventConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

import static com.example.URLShortener.analytic.AnalyticsStreamConstants.*;

@Slf4j
@Configuration
public class ClickStreamConfig {

    @Bean
    public StreamMessageListenerContainer<String, ObjectRecord<String, String>> clickStreamContainer(
            RedisConnectionFactory connectionFactory,
            StringRedisTemplate redis,
            ClickEventConsumer consumer) {

        ensureGroup(redis);

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        // Must stay below the Lettuce command timeout (default 2s), otherwise the
                        // blocking XREADGROUP outlives the command deadline and every poll fails
                        // with RedisCommandTimeoutException. 1s block + short idle re-poll is fine.
                        .pollTimeout(Duration.ofSeconds(1))
                        .targetType(String.class)
                        .build();

        StreamMessageListenerContainer<String, ObjectRecord<String, String>> container =
                StreamMessageListenerContainer.create(
                        connectionFactory,
                        options
                );

        container.receive(
                Consumer.from(GROUP, CONSUMER),
                StreamOffset.create(
                        STREAM_KEY,
                        ReadOffset.lastConsumed()
                ),
                consumer
        );

        container.start();

        log.info(
                "Click event stream consumer started: stream={}, group={}, consumer={}",
                STREAM_KEY,
                GROUP,
                CONSUMER
        );

        return container;
    }

    private void ensureGroup(StringRedisTemplate redis) {
        try {
            redis.opsForStream().createGroup(
                    STREAM_KEY,
                    ReadOffset.from("0-0"),
                    GROUP
            );

            log.info(
                    "Created consumer group: group={}, stream={}",
                    GROUP,
                    STREAM_KEY
            );

        } catch (Exception e) {
            // Spring wraps the Lettuce error, so "BUSYGROUP" is on a cause, not e.getMessage()
            // ("Error in execution"). Walk the whole cause chain before deciding to rethrow.
            if (isBusyGroup(e)) {
                log.debug(
                        "Consumer group already exists: group={}, stream={}",
                        GROUP,
                        STREAM_KEY
                );
            } else {
                throw e;
            }
        }
    }

    private static boolean isBusyGroup(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains("BUSYGROUP")) {
                return true;
            }
        }
        return false;
    }
}