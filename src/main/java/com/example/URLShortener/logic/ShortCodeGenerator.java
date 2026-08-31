package com.example.URLShortener.logic;

import com.example.URLShortener.repository.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(ShortCodeGenerator.class);

    private final Base62Encoder base62Encoder;
    private final CounterPermutation counterPermutation;
    private final CounterAllocator counterAllocator;
    private final ShortUrlRepository shortUrlRepository;

    ShortCodeGenerator(
            Base62Encoder base62Encoder,
            @Qualifier("postgresCounter")
            CounterAllocator counterAllocator,
            ShortUrlRepository shortUrlRepository,
            CounterPermutation counterPermutation) {
        this.base62Encoder = base62Encoder;
        this.counterAllocator = counterAllocator;
        this.counterPermutation = counterPermutation;
        this.shortUrlRepository = shortUrlRepository;
    }

    public String next() {
        String code;
        do {
            long value = counterAllocator.next();
            long permutedValue = counterPermutation.permute(value);
            code = base62Encoder.encode(permutedValue);
            log.debug("Generated candidate short code after counter {} and permutation: {}", value, code);
        } while (shortUrlRepository.existsByShortCode(code));
        return code;
    }
}