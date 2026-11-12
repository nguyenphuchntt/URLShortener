package com.example.URLShortener.analytic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.example.URLShortener.analytic.AnalyticsStreamConstants.DLQ_STREAM_KEY;
import static com.example.URLShortener.analytic.AnalyticsStreamConstants.GROUP;
import static com.example.URLShortener.analytic.AnalyticsStreamConstants.PAYLOAD_FIELD;
import static com.example.URLShortener.analytic.AnalyticsStreamConstants.STREAM_KEY;

/**
 * Reads click events from the stream with manual acknowledgement:
 * <ul>
 *   <li>processed or deliberately skipped → XACK</li>
 *   <li>malformed payload (will never succeed) → dead-letter stream + XACK</li>
 *   <li>processing failed (e.g. DB down) → no ACK; the entry stays in the PEL and
 *       {@link PendingClickEventReclaimer} retries it later</li>
 * </ul>
 * Never throws back into the listener container, so a failure cannot cancel the subscription.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClickEventConsumer implements StreamListener<String, ObjectRecord<String, String>> {

    private final ClickEventProcessor processor;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        try {
            handle(message.getId(), message.getValue());
        } catch (Exception e) {
            // Typically XACK/XADD failing because Redis is unreachable; the entry stays pending.
            log.error("Click event {} not acknowledged, will be retried", message.getId(), e);
        }
    }

    /**
     * Processes one stream entry.
     *
     * @return {@code true} if the entry was acknowledged (done, skipped or dead-lettered),
     *         {@code false} if it was left pending for a retry
     */
    public boolean handle(RecordId id, String json) {
        ClickEventPayload payload;
        try {
            if (json == null) {
                throw new IllegalArgumentException("missing '" + PAYLOAD_FIELD + "' field");
            }
            payload = objectMapper.readValue(json, ClickEventPayload.class);
        } catch (Exception e) {
            log.error("Malformed click event {}, moving to dead-letter stream", id, e);
            deadLetter(id, json, "malformed payload: " + e.getMessage());
            return true;
        }

        try {
            processor.process(payload);
        } catch (Exception e) {
            log.warn("Click event {} failed, left pending for retry: {}", id, e.toString());
            return false;
        }

        acknowledge(id);
        return true;
    }

    /** Copies the entry to the dead-letter stream, then acknowledges it on the main stream. */
    public void deadLetter(RecordId id, String json, String reason) {
        Map<String, String> fields = new HashMap<>();
        fields.put("originalId", id.getValue());
        fields.put("reason", reason == null ? "unknown" : reason);
        fields.put("failedAt", Instant.now().toString());
        if (json != null) {
            fields.put(PAYLOAD_FIELD, json);
        }
        redis.opsForStream().add(StreamRecords.newRecord().in(DLQ_STREAM_KEY).ofMap(fields));
        acknowledge(id);
    }

    private void acknowledge(RecordId id) {
        redis.opsForStream().acknowledge(STREAM_KEY, GROUP, id);
    }
}
