package com.example.URLShortener.service;

import com.example.URLShortener.entity.ShortUrl;

import java.util.List;

public interface ShortUrlService {

    Boolean create(String originUrl, Long ownerId);

    ShortUrl getByCode(String shortCode);

    List<ShortUrl> getAllByOwner(Long ownerId);

    Boolean  delete(String shortCode, Long ownerId);

    String resolveForRedirect(String shortCode);
}
