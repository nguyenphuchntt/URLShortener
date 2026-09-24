package com.example.URLShortener.analytic;

import com.example.URLShortener.repository.ClickEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClickEventCleanupJob {

    private final ClickEventRepository clickEventRepository;

    @Value("${analytics.click-event-retention-days:60}")
    private int retentionDays;

    @Scheduled(cron = "${analytics.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void purgeOldClickEvents() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
            int deletedCount = clickEventRepository.deleteOlderThan(cutoff);
            log.info("Purged {} click_event rows older than {}", deletedCount, cutoff);
        } catch (Exception e) {
            log.error("Failed to purge old click_event rows", e);
        }
    }
}
