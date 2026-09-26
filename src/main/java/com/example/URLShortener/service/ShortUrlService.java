package com.example.URLShortener.service;

import com.example.URLShortener.dto.request.CreateShortUrlRequest;
import com.example.URLShortener.dto.request.UpdateShortUrlRequest;
import com.example.URLShortener.dto.response.ShortUrlResponse;
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.entity.enums.ShortUrlStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ShortUrlService {

    ShortUrl create(CreateShortUrlRequest request);

    Optional<ShortUrl> getByCodeForRedirect(String shortCode);

    ShortUrl delete(String shortCode);

    Optional<ShortUrl> update(UpdateShortUrlRequest request);

    Page<ShortUrlResponse> getMyUrls(Pageable pageable);

    long countByOwner();
}
