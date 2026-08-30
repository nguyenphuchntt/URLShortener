package com.example.URLShortener.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GlobalCounterRepository {

    private final EntityManager entityManager;

    public Long getNextValue() {
        Number result = (Number) entityManager
                .createNativeQuery(
                        "SELECT nextval('short_code_counter')"
                )
                .getSingleResult();
        return result.longValue();
    }
}