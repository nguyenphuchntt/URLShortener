package com.example.URLShortener.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "link_stats")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "of")
@Builder
public class LinkStats {

    @Id
    @Column(name = "short_url_id")
    private Long shortUrlId;

    @Column(name = "total_clicks", nullable = false)
    private Long totalClicks = 0L;

    @Column(name = "last_click_at")
    private Instant lastClickAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "short_url_id", insertable = false, updatable = false)
    private ShortUrl shortUrl;

    public static LinkStats of(Long shortUrlId, Long totalClicks, Instant lastClickAt) {
        return new LinkStats(shortUrlId, totalClicks, lastClickAt, null);
    }
}
