package com.example.URLShortener.analytic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.example.URLShortener.analytic.AnalyticsStreamConstants.GROUP;
import static com.example.URLShortener.analytic.AnalyticsStreamConstants.PAYLOAD_FIELD;
import static com.example.URLShortener.analytic.AnalyticsStreamConstants.STREAM_KEY;

/**
 * Retries click events stuck in the consumer group's Pending Entries List.
 * <p>
 * An entry is pending when it was delivered but never acknowledged: processing failed, or the
 * instance that read it crashed. Every run lists entries idle longer than {@code minIdle}
 * (from any consumer, including dead instances), then:
 * <ul>
 *   <li>delivery count ≥ {@code maxDeliveries} → dead-letter stream + ACK</li>
 *   <li>otherwise → XCLAIM to this consumer and process again</li>
 * </ul>
 * XCLAIM honours {@code minIdle}, so when several instances race for the same entry only one wins.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingClickEventReclaimer {

    private final StringRedisTemplate redis;
    private final ClickEventConsumer consumer;
    private final ClickStreamProperties props;

    @Scheduled(
            initialDelayString = "${analytics.stream.reclaim-interval-ms:30000}",
            fixedDelayString = "${analytics.stream.reclaim-interval-ms:30000}")
    public void reclaim() {
        try {
            int handled = reclaimOnce();
            if (handled > 0) {
                log.info("Reclaimed {} pending click event(s)", handled);
            }
        } catch (Exception e) {
            log.warn("Pending click event reclaim failed: {}", e.toString());
        }
    }

    /** @return number of pending entries retried or dead-lettered */
    int reclaimOnce() {
        StreamOperations<String, Object, Object> ops = redis.opsForStream();
        PendingMessages pending = ops.pending(
                STREAM_KEY, GROUP, Range.unbounded(), props.batchSize(), props.minIdle());

        int handled = 0;
        for (PendingMessage pm : pending) {
            RecordId id = pm.getId();

            if (pm.getTotalDeliveryCount() >= props.maxDeliveries()) {
                log.error("Click event {} failed {} deliveries, moving to dead-letter stream",
                        id, pm.getTotalDeliveryCount());
                consumer.deadLetter(id, readPayload(ops, id),
                        "retries exhausted after " + pm.getTotalDeliveryCount() + " deliveries");
                handled++;
                continue;
            }

            // Empty when another instance claimed it first; Redis also drops PEL entries whose
            // stream entry no longer exists.
            List<MapRecord<String, Object, Object>> claimed =
                    ops.claim(STREAM_KEY, GROUP, props.consumerName(), props.minIdle(), id);
            for (MapRecord<String, Object, Object> record : claimed) {
                consumer.handle(record.getId(), asString(record.getValue().get(PAYLOAD_FIELD)));
                handled++;
            }
        }
        return handled;
    }

    private static String readPayload(StreamOperations<String, Object, Object> ops, RecordId id) {
        List<MapRecord<String, Object, Object>> records =
                ops.range(STREAM_KEY, Range.closed(id.getValue(), id.getValue()));
        return records == null || records.isEmpty()
                ? null
                : asString(records.get(0).getValue().get(PAYLOAD_FIELD));
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
