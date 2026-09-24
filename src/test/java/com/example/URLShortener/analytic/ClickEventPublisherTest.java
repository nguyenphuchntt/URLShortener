package com.example.URLShortener.analytic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClickEventPublisherTest {

    private static final String JSON = "{\"shortUrlId\":1}";

    @Mock StringRedisTemplate redis;
    @Mock StreamOperations<String, String, String> streamOperations;
    @Mock ObjectMapper objectMapper;

    private ClickEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ClickEventPublisher(redis, objectMapper);
    }

    @Test
    void publishClickEvent_shouldAppendSerializedPayloadToStream() {
        ClickEventPayload payload = payload();
        when(objectMapper.writeValueAsString(payload)).thenReturn(JSON);
        when(redis.<String, String>opsForStream()).thenReturn(streamOperations);

        publisher.publishClickEvent(payload);

        ArgumentCaptor<MapRecord<String, String, String>> captor = recordCaptor();
        verify(streamOperations).add(captor.capture());
        MapRecord<String, String, String> record = captor.getValue();
        assertThat(record.getStream()).isEqualTo(AnalyticsStreamConstants.STREAM_KEY);
        assertThat(record.getValue()).containsEntry("payload", JSON);
    }

    @Test
    void publishClickEvent_whenSerializationFails_shouldSwallowAndNotTouchRedis() {
        ClickEventPayload payload = payload();
        when(objectMapper.writeValueAsString(payload)).thenThrow(new RuntimeException("cannot serialize"));

        assertThatCode(() -> publisher.publishClickEvent(payload)).doesNotThrowAnyException();
        verify(redis, never()).opsForStream();
    }

    @Test
    void publishClickEvent_whenRedisFails_shouldSwallowSoRedirectIsNotBlocked() {
        ClickEventPayload payload = payload();
        when(objectMapper.writeValueAsString(payload)).thenReturn(JSON);
        when(redis.<String, String>opsForStream()).thenReturn(streamOperations);
        when(streamOperations.add(any())).thenThrow(new RuntimeException("redis down"));

        assertThatCode(() -> publisher.publishClickEvent(payload)).doesNotThrowAnyException();
    }

    private static ClickEventPayload payload() {
        return ClickEventPayload.builder()
                .shortUrlId(1L)
                .ip("8.8.8.8")
                .timestamp(1_700_000_000_000L)
                .build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<MapRecord<String, String, String>> recordCaptor() {
        return ArgumentCaptor.forClass(MapRecord.class);
    }
}
