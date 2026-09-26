package com.example.URLShortener.cache;

import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.BooleanOutput;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class BloomFilterService {

    static final String REDIS_KEY = "urlshortener:bloom:shortcodes";
    static final String ERROR_RATE = "0.0001";
    static final String CAPACITY = "1000000";
    private static final String TEMP_KEY = REDIS_KEY + ":rebuilding";

    private final StringRedisTemplate redis;

    public BloomFilterService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void add(String shortCode) {
        try {
            executeBoolean(BF_ADD, REDIS_KEY, shortCode);
            log.debug("Added shortCode='{}' to Bloom filter", shortCode);
        } catch (Exception e) {
            log.warn("Failed to add shortCode='{}' to Bloom filter", shortCode, e);
        }
    }

    public boolean mightContain(String shortCode) {
        try {
            return Boolean.TRUE.equals(executeBoolean(BF_EXISTS, REDIS_KEY, shortCode));
        } catch (Exception e) {
            log.warn("Bloom filter check failed for shortCode='{}', assuming might-exist (fail-open)",
                    shortCode, e);
            return true;
        }
    }

    /** Build into a temporary key and atomically rename it over the live key. */
    public void rebuild(Iterable<String> allShortCodes) {
        try {
            if (Boolean.TRUE.equals(redis.hasKey(REDIS_KEY))) {
                log.info("Bloom filter already present in Redis, skipping rebuild");
                return;
            }

            log.info("Rebuilding Bloom filter from existing short codes...");
            redis.delete(TEMP_KEY); // clear any leftover from a crashed rebuild
            executeStatus(BF_RESERVE, TEMP_KEY, ERROR_RATE, CAPACITY);

            int count = 0;
            for (String code : allShortCodes) {
                executeBoolean(BF_ADD, TEMP_KEY, code);
                count++;
            }

            executeStatus(RENAME, TEMP_KEY, REDIS_KEY);
            log.info("Bloom filter rebuilt with {} short codes", count);
        } catch (Exception e) {
            log.error("Failed to rebuild Bloom filter", e);
        }
    }

    private Boolean executeBoolean(ProtocolKeyword command, String... args) {
        return redis.execute((RedisConnection c) -> dispatch(c, command, new BooleanOutput<>(ByteArrayCodec.INSTANCE), args));
    }

    private void executeStatus(ProtocolKeyword command, String... args) {
        redis.execute((RedisConnection c) -> dispatch(c, command, new StatusOutput<>(ByteArrayCodec.INSTANCE), args));
    }

    @SuppressWarnings("unchecked")
    private <T> T dispatch(
            RedisConnection connection,
            ProtocolKeyword command,
            CommandOutput<byte[], byte[], T> output,
            String... args) {
        try {
            // LettuceConnection.getNativeConnection() returns the async command set
            // (RedisAsyncCommandsImpl), not a StatefulRedisConnection — dispatch on it directly.
            RedisClusterAsyncCommands<byte[], byte[]> nativeCommands =
                    (RedisClusterAsyncCommands<byte[], byte[]>) connection.getNativeConnection();
            CommandArgs<byte[], byte[]> commandArgs = new CommandArgs<>(ByteArrayCodec.INSTANCE);
            for (String arg : args) {
                commandArgs.add(b(arg));
            }
            return nativeCommands.dispatch(command, output, commandArgs).get();
        } catch (Exception e) {
            throw new IllegalStateException("RedisBloom command failed: " + command, e);
        }
    }

    private static final ProtocolKeyword BF_ADD = keyword("BF.ADD");
    private static final ProtocolKeyword BF_EXISTS = keyword("BF.EXISTS");
    private static final ProtocolKeyword BF_RESERVE = keyword("BF.RESERVE");
    private static final ProtocolKeyword RENAME = keyword("RENAME");

    private static ProtocolKeyword keyword(String name) {
        return new ProtocolKeyword() {
            private final byte[] bytes = name.getBytes(StandardCharsets.US_ASCII);

            @Override
            public byte[] getBytes() {
                return bytes;
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    private static byte[] b(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
