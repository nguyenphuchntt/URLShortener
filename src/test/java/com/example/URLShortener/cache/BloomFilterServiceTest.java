package com.example.URLShortener.cache;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RedisBloom commands are dispatched through the native Lettuce connection with an explicit
 * output decoder, because the RESP3 boolean/integer replies cannot be read by Spring's generic
 * execute(byte[]...). These tests capture the wire command and control the reply.
 */
@ExtendWith(MockitoExtension.class)
class BloomFilterServiceTest {

    private static final String KEY = "urlshortener:bloom:shortcodes";
    private static final String TEMP_KEY = KEY + ":rebuilding";

    @Mock StringRedisTemplate redis;
    @Mock RedisConnection connection;
    @Mock RedisClusterAsyncCommands<byte[], byte[]> nativeCommands;
    @Mock RedisFuture<Object> future;
    @Mock ValueOperations<String, String> valueOps;

    private BloomFilterService service;

    @BeforeEach
    void setUp() {
        service = new BloomFilterService(redis);
    }

    @Test
    void add_shouldDispatchBfAddAgainstTheLiveKey() {
        givenDispatchReplies(true);

        service.add("abc123");

        assertThat(capturedCommands()).containsExactly("BF.ADD");
    }

    @Test
    void add_whenRedisFails_shouldNotPropagate() {
        when(redis.execute(any(RedisCallback.class))).thenThrow(new RuntimeException("redis down"));

        assertThatCode(() -> service.add("abc123")).doesNotThrowAnyException();
    }

    @Test
    void mightContain_whenServerRepliesTrue_shouldReturnTrue() {
        givenDispatchReplies(true);

        assertThat(service.mightContain("abc123")).isTrue();
        assertThat(capturedCommands()).containsExactly("BF.EXISTS");
    }

    @Test
    void mightContain_whenServerRepliesFalse_shouldReturnFalse() {
        givenDispatchReplies(false);

        assertThat(service.mightContain("abc123")).isFalse();
    }

    @Test
    void mightContain_whenReplyIsNullOrWrongType_shouldReturnFalse() {
        givenDispatchReplies(null);

        assertThat(service.mightContain("abc123")).isFalse();
    }

    @Test
    void mightContain_whenRedisFails_shouldFailOpen() {
        when(redis.execute(any(RedisCallback.class))).thenThrow(new RuntimeException("redis down"));

        assertThat(service.mightContain("abc123")).isTrue();
    }

    @Test
    void rebuild_whenKeyAlreadyPresent_shouldSkipEntirely() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.hasKey(KEY)).thenReturn(true);

        service.rebuild(java.util.List.of("a", "b"));

        verify(redis, never()).execute(any(RedisCallback.class));
    }

    @Test
    void rebuild_whenKeyAbsent_shouldReserveAddAllThenSwap() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.hasKey(KEY)).thenReturn(false);
        givenDispatchReplies(null);

        service.rebuild(java.util.List.of("a", "b"));

        // BF.RESERVE, BF.ADD x2, RENAME — all four go through the native command dispatch.
        assertThat(capturedCommands())
                .containsExactly("BF.RESERVE", "BF.ADD", "BF.ADD", "RENAME");
        verify(redis).delete(TEMP_KEY);
    }

    @Test
    void rebuild_shouldNeverDeleteTheLiveKey() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.hasKey(KEY)).thenReturn(false);
        givenDispatchReplies(null);

        service.rebuild(java.util.List.of("a"));

        // del(TEMP_KEY) is expected; the live key is swapped in via RENAME, never deleted.
        verify(redis, never()).delete(KEY);
    }

    @Test
    void rebuild_whenRedisFails_shouldNotPropagate() {
        when(redis.hasKey(KEY)).thenThrow(new RuntimeException("redis down"));

        assertThatCode(() -> service.rebuild(java.util.List.of("a"))).doesNotThrowAnyException();
    }

    // ──────────────────────────── helpers ────────────────────────────

    @SuppressWarnings("unchecked")
    private void givenDispatchReplies(Object reply) {
        when(connection.getNativeConnection()).thenReturn(nativeCommands);
        when(nativeCommands.dispatch(any(ProtocolKeyword.class), any(CommandOutput.class), any(CommandArgs.class)))
                .thenReturn(future);
        try {
            when(future.get()).thenReturn(reply);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        when(redis.execute(any(RedisCallback.class))).thenAnswer(inv -> {
            RedisCallback<?> callback = inv.getArgument(0);
            return callback.doInRedis(connection);
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private java.util.List<String> capturedCommands() {
        ArgumentCaptor<ProtocolKeyword> captor = ArgumentCaptor.forClass(ProtocolKeyword.class);
        verify(nativeCommands, org.mockito.Mockito.atLeastOnce())
                .dispatch(captor.capture(), any(CommandOutput.class), any(CommandArgs.class));
        return captor.getAllValues().stream().map(ProtocolKeyword::toString).toList();
    }
}
