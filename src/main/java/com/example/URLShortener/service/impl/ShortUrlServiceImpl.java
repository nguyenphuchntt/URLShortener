package com.example.URLShortener.service.impl;

import com.example.URLShortener.dto.request.CreateShortUrlRequest;
import com.example.URLShortener.dto.request.UpdateShortUrlRequest;
import com.example.URLShortener.dto.response.ShortUrlResponse;
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.entity.enums.ShortUrlStatus;
import com.example.URLShortener.exception.*;
import com.example.URLShortener.logic.ShortCodeGenerator;
import com.example.URLShortener.repository.ShortUrlRepository;
import com.example.URLShortener.security.CurrentUser;
import com.example.URLShortener.service.ShortUrlService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ShortUrlServiceImpl implements ShortUrlService {

    private static final Logger log = LoggerFactory.getLogger(ShortUrlServiceImpl.class);

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

    @Transactional
    public ShortUrl create(CreateShortUrlRequest request) {
        if (request.getCustomShortCode() != null && !request.getCustomShortCode().isEmpty()) {
            if (shortUrlRepository.existsByShortCode(request.getCustomShortCode())) {
                throw new ShortCodeAlreadyUsed("Short code was used");
            } else {
                LocalDateTime now = LocalDateTime.now();
                ShortUrl newShortUrl = ShortUrl.builder()
                        .ownerId(currentUser.requireUserId())
                        .shortCode(request.getCustomShortCode())
                        .originUrl(request.getOriginUrl())
                        .status(ShortUrlStatus.ACTIVE)
                        .expiresAt(request.getExpiresAt())
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

                newShortUrl = shortUrlRepository.save(newShortUrl);
                log.info("Created new shortCode {} for origin URL {}, customized", newShortUrl.getShortCode(), newShortUrl.getOriginUrl());
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
                .updatedAt(LocalDateTime.now())
                .build();

        newShortUrl = shortUrlRepository.save(newShortUrl);
        log.info("Created new shortCode {} for origin URL {}", newShortUrl.getShortCode(), newShortUrl.getOriginUrl());
        return newShortUrl;
    }

    @Override
    public Optional<ShortUrl> getByCodeForRedirect(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short code not found"));
        if (shortUrl.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlExpiredException("Short code expired");
        }
        return Optional.of(shortUrl);
    }

    @Override
    @Transactional
    public ShortUrl delete(String shortCode) {
        Long userId = currentUser.requireUserId();
        ShortUrl url = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short code invalid"));
        if (!url.getOwnerId().equals(userId)) {
            throw new UnauthorizedException("Not owner of this shortcode");
        } else if (url.getStatus() == ShortUrlStatus.DELETED) {
            throw new ConflictException("Shortcode already deleted");
        } else {
            url.setStatus(ShortUrlStatus.DELETED);
            url = shortUrlRepository.save(url);
            log.info("Soft deleted short code {} of original URL {}", url.getShortCode(), url.getOriginUrl());
        }
        return url;
    }

    @Override
    @Transactional
    public Optional<ShortUrl> update(UpdateShortUrlRequest request) {
        final boolean[] modified = {false};
        return shortUrlRepository.findByShortCode(request.getShortCode())
                .filter(url -> url.getOwnerId().equals(currentUser.requireUserId())
                        && url.getStatus() != ShortUrlStatus.DELETED)
                .map(url -> {
                    if (request.getOriginUrl() != null && !request.getOriginUrl().equals(url.getOriginUrl())) {
                        throw new IllegalArgumentException("Original url and short code miss matched");
                    }
                    if (request.getNewShortCode() != null && !request.getNewShortCode().isEmpty()
                            && !request.getNewShortCode().equals(request.getShortCode())) {
                        if (shortUrlRepository.existsByShortCode(request.getNewShortCode())) {
                            throw new ShortCodeAlreadyUsed("Short code was used");
                        }
                        if (url.getShortCode().equals(request.getNewShortCode())) {
                            throw new ConflictException("Short code was the same");
                        }
                        url.setShortCode(request.getNewShortCode());
                        modified[0] = true;
                    }
                    if (request.getStatus() != null && request.getStatus() != url.getStatus()) {
                        url.setStatus(request.getStatus());
                        modified[0] = true;
                    }
                    if (request.getExpiresAt() != null && request.getExpiresAt() != url.getExpiresAt()) {
                        url.setExpiresAt(request.getExpiresAt());
                        modified[0] = true;
                    }
                    if (modified[0]) {
                        url.setUpdatedAt(LocalDateTime.now());
                    }
                    return shortUrlRepository.save(url);
                });
    }

    @Override
    public Page<ShortUrlResponse> getMyUrls(Pageable pageable) {
        Long userId = currentUser.requireUserId();
        return shortUrlRepository
                .findAllByOwnerId(userId, pageable)
                .map(
                        shortUrl -> {
                            return ShortUrlResponse.builder()
                                    .originUrl(shortUrl.getOriginUrl())
                                    .shortCode(shortUrl.getShortCode())
                                    .updatedAt(shortUrl.getUpdatedAt())
                                    .createdAt(shortUrl.getCreatedAt())
                                    .expiresAt(shortUrl.getExpiresAt())
                                    .build();
                        }
                );
    }

    @Override
    public long countByOwner() {
        Long userId = currentUser.requireUserId();
        return shortUrlRepository.countByOwnerId(userId);
    }
}
