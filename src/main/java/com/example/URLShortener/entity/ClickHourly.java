package com.example.URLShortener.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "clicks_hourly")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "of")
@Builder
public class ClickHourly {

    @EmbeddedId
    private ClickHourlyId id;

    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "short_url_id", insertable = false, updatable = false)
    private ShortUrl shortUrl;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class ClickHourlyId implements java.io.Serializable {
        @Column(name = "short_url_id")
        private Long shortUrlId;

        @Column(name = "hour")
        private Instant hour;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClickHourlyId that)) return false;
            return Objects.equals(shortUrlId, that.shortUrlId)
                    && Objects.equals(hour, that.hour);
        }

        @Override
        public int hashCode() {
            return Objects.hash(shortUrlId, hour);
        }
    }

    public static ClickHourly of(Long shortUrlId, Instant hour, Long clickCount) {
        return new ClickHourly(ClickHourlyId.of(shortUrlId, hour), clickCount, null);
    }
}
