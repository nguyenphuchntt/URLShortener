package com.example.URLShortener.logic;

import org.springframework.stereotype.Component;

@Component
public class CounterPermutation {

    private static final int ROUNDS = 5;

    private static final int[] ROUND_KEYS = {
            0x1234,
            0x5678,
            0x9ABC,
            0xDEF0,
            0x1357
    };

    public long permute(long counter) {
        if (counter < 0 || counter > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("Counter must be in range 0..2^32-1");
        }

        int value = (int) counter;

        int left = (value >>> 16) & 0xFFFF;
        int right = value & 0xFFFF;

        for (int i = 0; i < ROUNDS; i++) {
            int newLeft = right;
            int newRight = left ^ roundFunction(right, ROUND_KEYS[i]);
            left = newLeft;
            right = newRight & 0xFFFF;
        }

        int result = (left << 16) | right;

        return Integer.toUnsignedLong(result);
    }

    public long inverse(long value) {
        if (value < 0 || value > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("Value must be in range 0..2^32-1");
        }

        int intValue = (int) value;

        int left = (intValue >>> 16) & 0xFFFF;
        int right = intValue & 0xFFFF;

        for (int i = ROUNDS - 1; i >= 0; i--) {
            int oldRight = left;
            int oldLeft = right ^ roundFunction(left, ROUND_KEYS[i]);

            left = oldLeft & 0xFFFF;
            right = oldRight & 0xFFFF;
        }

        int result = (left << 16) | right;

        return Integer.toUnsignedLong(result);
    }

    private int roundFunction(int right, int key) {
        int x = (right ^ key) & 0xFFFF;

        x *= 0x9E37;
        x ^= x >>> 8;

        return x & 0xFFFF;
    }
}