package com.example.URLShortener.logic;

import com.example.URLShortener.repository.GlobalCounterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("postgresCounter")
public class PostgresSequenceCounterAllocator implements CounterAllocator {

    private static final Logger log = LoggerFactory.getLogger(PostgresSequenceCounterAllocator.class);

    private final GlobalCounterRepository globalCounterRepository;

    public PostgresSequenceCounterAllocator(
            GlobalCounterRepository globalCounterRepository) {
        this.globalCounterRepository = globalCounterRepository;
    }

    public long next() {
        long nextValue = this.globalCounterRepository.getNextValue();
        log.debug("Fetched next counter value from Postgres: {}", nextValue);
        return nextValue;
    }
}
