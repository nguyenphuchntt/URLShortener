package com.example.URLShortener.service;

import com.example.URLShortener.entity.ShortUrl;

import java.util.List;

public interface ShortUrlService {

    ShortUrl getByCode(String shortCode);

    List<ShortUrl> getAllByOwner(Long ownerId);

    void delete(String shortCode, Long ownerId);

    String resolveForRedirect(String shortCode);
}
