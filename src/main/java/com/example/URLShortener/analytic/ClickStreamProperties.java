package com.example.URLShortener.analytic;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.InetAddress;
import java.time.Duration;
import java.util.UUID;

/**
 * Settings for the click-event stream consumer.
 *
 * @param consumerName  consumer name inside the group; must differ per app instance. Defaults to
 *                      {@code consumer-<hostname>} (container id under Docker), which stays stable across
 *                      restarts of the same container so it keeps ownership of its own pending entries.
 * @param minIdle       how long an entry must sit unacknowledged in the PEL before it is reclaimed and retried
 * @param maxDeliveries total delivery attempts (first read + retries) before an entry goes to the dead-letter stream
 * @param batchSize     max pending entries inspected per reclaim run
 */
@ConfigurationProperties(prefix = "analytics.stream")
public record ClickStreamProperties(
        String consumerName,
        Duration minIdle,
        int maxDeliveries,
        int batchSize
) {
    public ClickStreamProperties {
        if (consumerName == null || consumerName.isBlank()) {
            consumerName = defaultConsumerName();
        }
        if (minIdle == null || minIdle.isNegative() || minIdle.isZero()) {
            minIdle = Duration.ofSeconds(60);
        }
        if (maxDeliveries <= 0) {
            maxDeliveries = 5;
        }
        if (batchSize <= 0) {
            batchSize = 100;
        }
    }

    static String defaultConsumerName() {
        String host = System.getenv("HOSTNAME");
        if (host == null || host.isBlank()) {
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (Exception ignored) {
                host = null;
            }
        }
        if (host == null || host.isBlank()) {
            host = UUID.randomUUID().toString().substring(0, 8);
        }
        return "consumer-" + host;
    }
}
