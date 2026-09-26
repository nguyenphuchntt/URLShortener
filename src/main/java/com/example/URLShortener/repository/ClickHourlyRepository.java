package com.example.URLShortener.repository;

import com.example.URLShortener.entity.ClickHourly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ClickHourlyRepository extends JpaRepository<ClickHourly, ClickHourly.ClickHourlyId> {

    @Query("""
        SELECT ch FROM ClickHourly ch
        WHERE ch.id.shortUrlId = :shortUrlId
          AND ch.id.hour >= :fromHour
          AND ch.id.hour < :toHour
        ORDER BY ch.id.hour ASC
        """)
    List<ClickHourly> findByShortUrlIdAndHourBetween(
            @Param("shortUrlId") Long shortUrlId,
            @Param("fromHour") Instant fromHour,
            @Param("toHour") Instant toHour
    );

    @Query("""
        SELECT ch FROM ClickHourly ch
        WHERE ch.id.shortUrlId IN :shortUrlIds
          AND ch.id.hour >= :fromHour
          AND ch.id.hour < :toHour
        ORDER BY ch.id.hour ASC
        """)
    List<ClickHourly> findByShortUrlIdsAndHourBetween(
            @Param("shortUrlIds") List<Long> shortUrlIds,
            @Param("fromHour") Instant fromHour,
            @Param("toHour") Instant toHour
    );
}
