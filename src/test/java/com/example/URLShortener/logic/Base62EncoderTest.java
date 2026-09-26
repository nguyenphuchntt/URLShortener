package com.example.URLShortener.logic;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62EncoderTest {

    private final Base62Encoder encoder = new Base62Encoder();

    @Test
    void encode_whenZero_shouldReturnZero() {
        assertThat(encoder.encode(0)).isEqualTo("0");
    }

    @Test
    void encodeAndDecode_whenValueIsValid_shouldRoundTrip() {
        for (long value : new long[]{1, 61, 62, 123456789L, Long.MAX_VALUE}) {
            assertThat(encoder.decode(encoder.encode(value))).isEqualTo(value);
        }
    }

    @Test
    void encode_whenValueIsNegative_shouldThrow() {
        assertThatThrownBy(() -> encoder.encode(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decode_whenValueIsNullOrEmpty_shouldThrow() {
        assertThatThrownBy(() -> encoder.decode(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> encoder.decode("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decode_whenCharacterIsInvalid_shouldThrow() {
        assertThatThrownBy(() -> encoder.decode("invalid!"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
