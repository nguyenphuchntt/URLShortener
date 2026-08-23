package com.example.URLShortener.logic;

import org.springframework.stereotype.Component;

@Component
public class CounterPermutation {
    // Feistel Network

    public static final int ROUNDS = 5;
    private static final long[] ROUND_KEYS = {
            0x12345678L,
            0x87654321L,
            0xABCDEF01L,
            0x10203040L,
            0x55667788L,
            0xCAFEBABEL,
            0x13579BDFL,
            0x2468ACE0L
    };

    public long permute(long counter) {
        int left = (int)(counter >>> 32);
        int right = (int)counter;

        for (int i = 0; i < ROUNDS; i++) {
            int newLeft = right;
            int newRight = left ^ roundFunction(right, ROUND_KEYS[i]);
            left = newLeft;
            right = newRight;
        }

        return ((long) left << 32) | (right & 0xFFFFFFFFL);
    }

    public long inverse(long value) {
        int left = (int) (value >>> 32);
        int right = (int) value;

        for (int i = ROUNDS - 1; i >= 0; i--) {
            int oldRight = left;
            int oldLeft = right ^ roundFunction(left, ROUND_KEYS[i]);

            left = oldLeft;
            right = oldRight;
        }

        return ((long) left << 32) | (right & 0xFFFFFFFFL);
    }

    private int roundFunction(int right, long key) {
        long x = (right & 0xFFFFFFFFL) ^ key;

        x *= 0x9E3779B9L;
        x ^= x >>> 16;

        return (int) x;
    }
}
