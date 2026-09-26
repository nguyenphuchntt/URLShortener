package com.example.URLShortener.logic;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CounterPermutationTest {

    private final CounterPermutation permutation = new CounterPermutation();

    @Test
    void inverse_whenValueWasPermuted_shouldReturnOriginalValue() {
        for (long value : new long[]{0, 1, 42, 65535, 65536, 123456789L, 0xFFFFFFFFL}) {
            assertThat(permutation.inverse(permutation.permute(value))).isEqualTo(value);
        }
    }

    @Test
    void permute_whenValueIsOutsideUnsignedIntRange_shouldThrow() {
        assertThatThrownBy(() -> permutation.permute(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> permutation.permute(0x1_0000_0000L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inverse_whenValueIsOutsideUnsignedIntRange_shouldThrow() {
        assertThatThrownBy(() -> permutation.inverse(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> permutation.inverse(0x1_0000_0000L)).isInstanceOf(IllegalArgumentException.class);
    }
}
