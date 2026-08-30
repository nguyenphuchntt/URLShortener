package com.example.URLShortener.service.impl;

import com.example.URLShortener.dto.request.CreateShortUrlRequest;
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.entity.enums.ShortUrlStatus;
import com.example.URLShortener.exception.ShortCodeAlreadyUsed;
import com.example.URLShortener.logic.ShortCodeGenerator;
import com.example.URLShortener.repository.ShortUrlRepository;
import com.example.URLShortener.security.CurrentUser;
import com.example.URLShortener.service.ShortUrlService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShortUrlServiceImpl implements ShortUrlService {

    private final ShortCodeGenerator shortCodeGenerator;
    private final ShortUrlRepository shortUrlRepository;
    private final CurrentUser currentUser;

    ShortUrlServiceImpl (
            ShortUrlRepository shortUrlRepository,
            CurrentUser currentUser,
            ShortCodeGenerator shortCodeGenerator) {
        this.shortCodeGenerator = shortCodeGenerator;
        this.shortUrlRepository = shortUrlRepository;
        this.currentUser = currentUser;
    }

    public ShortUrl create(CreateShortUrlRequest request) {
        if (request.getCustomShortCode() != null && !request.getCustomShortCode().isEmpty()) {
            if (shortUrlRepository.existsByShortCode(request.getCustomShortCode())) {
                throw new ShortCodeAlreadyUsed("Short code was used");
            } else {
                ShortUrl newShortUrl = ShortUrl.builder()
                        .ownerId(currentUser.requireUserId())
                        .shortCode(request.getCustomShortCode())
                        .originUrl(request.getOriginUrl())
                        .status(ShortUrlStatus.ACTIVE)
                        .expiresAt(request.getExpiresAt())
                        .createdAt(LocalDateTime.now())
                        .build();

                newShortUrl = shortUrlRepository.save(newShortUrl);
                return newShortUrl;
            }
        }

        String originUrl = request.getOriginUrl();
        String shortCode = shortCodeGenerator.next();

        Long ownerId = currentUser.requireUserId();

        ShortUrl newShortUrl = ShortUrl.builder()
                .ownerId(ownerId)
                .shortCode(shortCode)
                .originUrl(originUrl)
                .status(ShortUrlStatus.ACTIVE)
                .expiresAt(request.getExpiresAt())
                .createdAt(LocalDateTime.now())
                .build();

        newShortUrl = shortUrlRepository.save(newShortUrl);

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
