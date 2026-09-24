package com.example.URLShortener.analytic;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.example.URLShortener.analytic.AnalyticsStreamConstants.STREAM_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClickEventPublisher {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public void publishClickEvent(ClickEventPayload event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            MapRecord<String, String, String> record = StreamRecords.newRecord()
                    // stream_key -> {entry_id: {"payload": json}}
                    // string         string     string
                    .in(STREAM_KEY)
                    .ofMap(Map.of("payload", json));
            redis.opsForStream().add(record);
            log.debug("Published click event for shortUrlId={}", event.getShortUrlId());
        } catch (Exception e) {
            log.error("Failed to publish click event for shortUrlId={}", event.getShortUrlId(), e);
        }
    }
}