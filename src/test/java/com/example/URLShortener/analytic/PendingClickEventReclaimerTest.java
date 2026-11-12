package com.example.URLShortener.analytic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.example.URLShortener.analytic.AnalyticsStreamConstants.GROUP;
import static com.example.URLShortener.analytic.AnalyticsStreamConstants.STREAM_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingClickEventReclaimerTest {

    private static final Duration MIN_IDLE = Duration.ofSeconds(60);
    private static final RecordId ID = RecordId.of("1700000000000-0");
    private static final String JSON = "{\"shortUrlId\":1}";

    @Mock StringRedisTemplate redis;
    @Mock StreamOperations<String, Object, Object> streamOps;
    @Mock ClickEventConsumer consumer;

    private PendingClickEventReclaimer reclaimer;

    @BeforeEach
    void setUp() {
        ClickStreamProperties props = new ClickStreamProperties("consumer-test", MIN_IDLE, 5, 100);
        reclaimer = new PendingClickEventReclaimer(redis, consumer, props);
        when(redis.opsForStream()).thenReturn(streamOps);
    }

    @Test
    void reclaimOnce_whenBelowMaxDeliveries_shouldClaimAndReprocess() {
        givenPending(pending(2));
        MapRecord<String, Object, Object> record =
                StreamRecords.newRecord().in(STREAM_KEY).withId(ID).ofMap(Map.<Object, Object>of("payload", JSON));
        when(streamOps.claim(STREAM_KEY, GROUP, "consumer-test", MIN_IDLE, ID)).thenReturn(List.of(record));

        assertThat(reclaimer.reclaimOnce()).isEqualTo(1);

        verify(consumer).handle(ID, JSON);
        verify(consumer, never()).deadLetter(any(), any(), any());
    }

    @Test
    void reclaimOnce_whenClaimedByAnotherInstance_shouldSkip() {
        givenPending(pending(2));
        when(streamOps.claim(STREAM_KEY, GROUP, "consumer-test", MIN_IDLE, ID)).thenReturn(List.of());

        assertThat(reclaimer.reclaimOnce()).isZero();

        verify(consumer, never()).handle(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void reclaimOnce_whenMaxDeliveriesReached_shouldDeadLetterWithoutClaim() {
        givenPending(pending(5));
        MapRecord<String, Object, Object> record =
                StreamRecords.newRecord().in(STREAM_KEY).withId(ID).ofMap(Map.<Object, Object>of("payload", JSON));
        when(streamOps.range(eq(STREAM_KEY), any(Range.class))).thenReturn(List.of(record));

        assertThat(reclaimer.reclaimOnce()).isEqualTo(1);

        verify(consumer).deadLetter(eq(ID), eq(JSON), any());
        verify(streamOps, never()).claim(any(), any(), any(), any(Duration.class), any(RecordId[].class));
        verify(consumer, never()).handle(any(), any());
    }

    @SuppressWarnings("unchecked")
    private void givenPending(PendingMessage... messages) {
        when(streamOps.pending(eq(STREAM_KEY), eq(GROUP), any(Range.class), eq(100L), eq(MIN_IDLE)))
                .thenReturn(new PendingMessages(GROUP, List.of(messages)));
    }

    private static PendingMessage pending(long deliveryCount) {
        return new PendingMessage(ID, Consumer.from(GROUP, "consumer-dead"), MIN_IDLE.plusSeconds(5), deliveryCount);
    }
}
