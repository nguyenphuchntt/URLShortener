package com.example.URLShortener.repository;

import com.example.URLShortener.entity.LinkStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkStatsRepository extends JpaRepository<LinkStats, Long> {

    Optional<LinkStats> findByShortUrlId(Long shortUrlId);

    List<LinkStats> findByShortUrlIdIn(List<Long> shortUrlIds);
}
