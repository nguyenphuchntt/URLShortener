package com.example.URLShortener.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "click_country_stats")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "of")
@Builder
public class ClickCountryStats {

    @EmbeddedId
    private ClickCountryStatsId id;

    @Column(nullable = false)
    private Long clicks = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "short_url_id", insertable = false, updatable = false)
    private ShortUrl shortUrl;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class ClickCountryStatsId implements Serializable {
        @Column(name = "short_url_id")
        private Long shortUrlId;

        @Column(name = "stat_date")
        private LocalDate statDate;

        @Column(name = "country", length = 2)
        private String country;
    }

    public static ClickCountryStats of(Long shortUrlId, LocalDate statDate, String country, Long clicks) {
        return new ClickCountryStats(ClickCountryStatsId.of(shortUrlId, statDate, country), clicks, null);
    }
}
