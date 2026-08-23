package com.example.URLShortener.service;

import com.example.URLShortener.dto.request.CreateShortUrlRequest;
import com.example.URLShortener.entity.ShortUrl;

import java.util.List;

public interface ShortUrlService {

    ShortUrl create(CreateShortUrlRequest createShortUrlRequest);

    ShortUrl getByCode(String shortCode);

    List<ShortUrl> getAllByOwner(Long ownerId);

    Boolean  delete(String shortCode, Long ownerId);
}
