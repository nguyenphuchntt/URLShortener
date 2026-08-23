package com.example.URLShortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalCounterRepository extends JpaRepository<Object, Long> {

    @Query(
            value = "SELECT nextval('short_code_counter')",
            nativeQuery = true
    )
    Long getNextValue();
}
