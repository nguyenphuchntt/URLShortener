package com.example.URLShortener.exception;

public class ShortCodeAlreadyUsed extends RuntimeException {
    public ShortCodeAlreadyUsed(String message) {
        super(message);
    }
}
