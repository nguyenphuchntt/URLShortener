package com.example.URLShortener.analytic;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClickEventPublisher {

    private static final String STREAM_KEY = "urlshortener:stream:click_events";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public void publishClickEvent(ClickEventPayload event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            ObjectRecord<String, String> record = StreamRecords.newRecord()
                    .ofObject(json)
                    .withStreamKey(STREAM_KEY);
            redis.opsForStream().add(record);
            log.debug("Published click event for shortCode={}, shortUrlId={}", event.getShortCode(), event.getShortUrlId());
        } catch (Exception e) {
            log.error("Failed to publish click event for shortCode={}, shortUrlId={}",
                    event.getShortCode(), event.getShortUrlId(), e);
        }
    }
}
