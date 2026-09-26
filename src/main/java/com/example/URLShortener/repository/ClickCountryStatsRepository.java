package com.example.URLShortener.repository;

import com.example.URLShortener.entity.ClickCountryStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClickCountryStatsRepository extends JpaRepository<ClickCountryStats, ClickCountryStats.ClickCountryStatsId> {

    @Query("""
        SELECT ccs FROM ClickCountryStats ccs
        WHERE ccs.id.shortUrlId = :shortUrlId
          AND ccs.id.statDate >= :fromDate
          AND ccs.id.statDate <= :toDate
        ORDER BY ccs.clicks DESC, ccs.id.country ASC
        """)
    List<ClickCountryStats> findByShortUrlIdAndDateBetween(
            @Param("shortUrlId") Long shortUrlId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
