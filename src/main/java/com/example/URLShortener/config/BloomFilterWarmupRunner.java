package com.example.URLShortener.config;

import com.example.URLShortener.cache.BloomFilterService;
import com.example.URLShortener.repository.ClickEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BloomFilterWarmupRunner implements ApplicationRunner {

    private final BloomFilterService bloomFilterService;
    private final ClickEventRepository clickEventRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<String> allCodes = clickEventRepository.findAllShortCodes();
            bloomFilterService.rebuild(allCodes);
        } catch (Exception e) {
            log.error("Failed to warm up Bloom filter on startup", e);
        }
    }
}
