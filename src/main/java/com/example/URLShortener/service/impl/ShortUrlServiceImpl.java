package com.example.URLShortener.service.impl;

import com.example.URLShortener.dto.request.CreateShortUrlRequest;
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.logic.ShortCodeGenerator;
import com.example.URLShortener.repository.ShortUrlRepository;
import com.example.URLShortener.service.ShortUrlService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShortUrlServiceImpl implements ShortUrlService {

    private final ShortCodeGenerator shortCodeGenerator;
    private final ShortUrlRepository shortUrlRepository;

    ShortUrlServiceImpl (
            ShortUrlRepository shortUrlRepository,
            ShortCodeGenerator shortCodeGenerator) {
        this.shortCodeGenerator = shortCodeGenerator;
        this.shortUrlRepository = shortUrlRepository;
    }

    public ShortUrl create(CreateShortUrlRequest request) {
        String originUrl = request.getOriginUrl().trim();
        String shortCode = shortCodeGenerator.next();

        ShortUrl newShortUrl = ShortUrl.builder()
                .shortCode(shortCode)
                .originUrl(originUrl)
                .expiresAt(request.getExpiresAt())
                .createdAt(LocalDateTime.now())
                .build();

        shortUrlRepository.save(newShortUrl);

        return newShortUrl;
    }

    @Override
    public ShortUrl getByCode(String shortCode) {
        return null;
    }

    @Override
    public List<ShortUrl> getAllByOwner(Long ownerId) {
        return List.of();
    }

    @Override
    public Boolean delete(String shortCode, Long ownerId) {
        return null;
    }
}
