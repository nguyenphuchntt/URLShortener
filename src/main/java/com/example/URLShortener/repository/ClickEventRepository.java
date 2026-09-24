package com.example.URLShortener.repository;

import com.example.URLShortener.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO clicks_hourly (short_url_id, hour, click_count)
        VALUES (:shortUrlId, date_trunc('hour', CAST(:clickedAt AS timestamp)), 1)
        ON CONFLICT (short_url_id, hour)
        DO UPDATE SET click_count = clicks_hourly.click_count + 1
        """, nativeQuery = true)
    void upsertHourly(
            @Param("shortUrlId") Long shortUrlId,
            @Param("clickedAt") LocalDateTime clickedAt);

    @Modifying
    @Query(value = """
        INSERT INTO link_stats (short_url_id, total_clicks, last_click_at)
        VALUES (:shortUrlId, 1, :clickedAt)
        ON CONFLICT (short_url_id)
        DO UPDATE SET total_clicks = link_stats.total_clicks + 1,
                      last_click_at = EXCLUDED.last_click_at
        """, nativeQuery = true)
    void upsertLinkStats(
            @Param("shortUrlId") Long shortUrlId,
            @Param("clickedAt") LocalDateTime clickedAt);

    @Modifying
    @Query(value = """
        INSERT INTO click_country_stats (short_url_id, stat_date, country, clicks)
        VALUES (:shortUrlId, CAST(:statDate AS date), :country, 1)
        ON CONFLICT (short_url_id, stat_date, country)
        DO UPDATE SET clicks = click_country_stats.clicks + 1
        """, nativeQuery = true)
    void upsertCountryStats(
            @Param("shortUrlId") Long shortUrlId,
            @Param("statDate") LocalDateTime statDate,
            @Param("country") String country);

    @Modifying
    @Query(value = "DELETE FROM click_event WHERE clicked_at < :cutoff", nativeQuery = true)
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Query(value = "SELECT DISTINCT short_code FROM short_urls", nativeQuery = true)
    List<String> findAllShortCodes();
}
