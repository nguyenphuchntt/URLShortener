package com.example.URLShortener.logic;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final char[] alphabet = ALPHABET.toCharArray();

    public String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative value not supported");
        }
        if (value == 0) {
            return String.valueOf(alphabet[0]);
        }
        StringBuilder result = new StringBuilder();
        while (value > 0) {
            int remainder = (int) (value % 62);
            result.append(alphabet[remainder]);
            value /= 62;
        }
        return result.reverse().toString();
    }

    public long decode(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Value must not be empty");
        }
        long result = 0;
        for (char c : value.toCharArray()) {
            int digit = indexOf(c);
            if (digit == -1) {
                throw new IllegalArgumentException(
                        "Invalid Base62 character: " + c
                );
            }
            result = result * 62 + digit;
        }
        return result;
    }

    private int indexOf(char c) {
        for (int i = 0; i < alphabet.length; i++) {
            if (alphabet[i] == c) {
                return i;
            }
        }
        return -1;
    }

}
