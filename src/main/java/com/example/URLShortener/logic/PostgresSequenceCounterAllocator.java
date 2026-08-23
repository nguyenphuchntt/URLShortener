package com.example.URLShortener.logic;

import com.example.URLShortener.repository.GlobalCounterRepository;
import org.springframework.stereotype.Component;

@Component("postgresCounter")
public class PostgresSequenceCounterAllocator implements CounterAllocator {

    private final GlobalCounterRepository globalCounterRepository;

    public PostgresSequenceCounterAllocator(
            GlobalCounterRepository globalCounterRepository) {
        this.globalCounterRepository = globalCounterRepository;
    }

    public long next() {
        return this.globalCounterRepository.getNextValue();
    }
}
