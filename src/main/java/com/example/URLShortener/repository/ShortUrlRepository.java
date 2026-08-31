package com.example.URLShortener.repository;

import com.example.URLShortener.entity.ShortUrl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
    Page<ShortUrl> findAllByOwnerId(Long ownerId, Pageable pageable);

    @Query("""
    SELECT COUNT(s)
    FROM ShortUrl s
    WHERE s.ownerId = :ownerId
    """)
    long countByOwnerId(@Param("ownerId") Long ownerId);
}
