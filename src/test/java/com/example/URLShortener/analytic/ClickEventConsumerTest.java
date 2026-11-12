package com.example.URLShortener.analytic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import static com.example.URLShortener.analytic.AnalyticsStreamConstants.DLQ_STREAM_KEY;
import static com.example.URLShortener.analytic.AnalyticsStreamConstants.GROUP;
import static com.example.URLShortener.analytic.AnalyticsStreamConstants.STREAM_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClickEventConsumerTest {

    private static final RecordId ID = RecordId.of("1700000000000-0");
    private static final String JSON = "{\"shortUrlId\":1,\"ip\":\"8.8.8.8\",\"timestamp\":1700000000000}";

    @Mock ClickEventProcessor processor;
    @Mock StringRedisTemplate redis;
    @Mock StreamOperations<String, Object, Object> streamOps;

    private ClickEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ClickEventConsumer(processor, redis, new ObjectMapper());
        lenient().when(redis.opsForStream()).thenReturn(streamOps);
    }

    @Test
    void handle_whenProcessed_shouldAck() {
        assertThat(consumer.handle(ID, JSON)).isTrue();

        verify(processor).process(any(ClickEventPayload.class));
        verify(streamOps).acknowledge(STREAM_KEY, GROUP, ID);
        verify(streamOps, never()).add(any(MapRecord.class));
    }

    @Test
    void handle_whenProcessingFails_shouldLeavePendingWithoutAck() {
        doThrow(new DataAccessResourceFailureException("db down")).when(processor).process(any());

        assertThat(consumer.handle(ID, JSON)).isFalse();

        verify(streamOps, never()).acknowledge(any(String.class), any(String.class), any(RecordId[].class));
        verify(streamOps, never()).add(any(MapRecord.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handle_whenPayloadMalformed_shouldDeadLetterAndAck() {
        assertThat(consumer.handle(ID, "{not json")).isTrue();

        ArgumentCaptor<MapRecord<String, String, String>> dlq = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOps).add(dlq.capture());
        assertThat(dlq.getValue().getStream()).isEqualTo(DLQ_STREAM_KEY);
        assertThat(dlq.getValue().getValue())
                .containsEntry("originalId", ID.getValue())
                .containsEntry("payload", "{not json")
                .containsKey("reason");
        verify(streamOps).acknowledge(STREAM_KEY, GROUP, ID);
        verify(processor, never()).process(any());
    }

    @Test
    void handle_whenPayloadFieldMissing_shouldDeadLetterAndAck() {
        assertThat(consumer.handle(ID, null)).isTrue();

        verify(streamOps).add(any(MapRecord.class));
        verify(streamOps).acknowledge(STREAM_KEY, GROUP, ID);
    }

    @Test
    @SuppressWarnings("unchecked")
    void onMessage_whenAckFails_shouldNotThrowIntoContainer() {
        ObjectRecord<String, String> message = org.mockito.Mockito.mock(ObjectRecord.class);
        when(message.getId()).thenReturn(ID);
        when(message.getValue()).thenReturn(JSON);
        when(streamOps.acknowledge(STREAM_KEY, GROUP, ID))
                .thenThrow(new DataAccessResourceFailureException("redis down"));

        assertThatCode(() -> consumer.onMessage(message)).doesNotThrowAnyException();
    }
}
