package com.example.URLShortener.logic;

import org.springframework.stereotype.Component;

@Component
public interface CounterAllocator {
    long next();
}
