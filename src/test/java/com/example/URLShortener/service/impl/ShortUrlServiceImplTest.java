package com.example.URLShortener.service.impl;

import com.example.URLShortener.cache.UrlCacheService;
import com.example.URLShortener.dto.request.CreateShortUrlRequest;
import com.example.URLShortener.dto.request.UpdateShortUrlRequest;
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.entity.enums.ShortUrlStatus;
import com.example.URLShortener.exception.ConflictException;
import com.example.URLShortener.exception.ResourceNotFoundException;
import com.example.URLShortener.exception.ShortCodeAlreadyUsed;
import com.example.URLShortener.exception.UnauthorizedException;
import com.example.URLShortener.exception.UrlExpiredException;
import com.example.URLShortener.logic.ShortCodeGenerator;
import com.example.URLShortener.repository.ShortUrlRepository;
import com.example.URLShortener.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceImplTest {

    @Mock ShortUrlRepository repository;
    @Mock CurrentUser currentUser;
    @Mock ShortCodeGenerator generator;
    @Mock UrlCacheService urlCacheService;

    private ShortUrlServiceImpl service;

    @BeforeEach
    void setUp() {
        // getByCodeForRedirect đọc cache trước; mặc định coi như cache MISS để test đi nhánh DB.
        lenient().when(urlCacheService.get(anyString())).thenReturn(Optional.empty());
        service = new ShortUrlServiceImpl(repository, currentUser, urlCacheService, generator);
    }

    @Test
    void create_whenCustomCodeIsAvailable_shouldSaveActiveUrl() {
        CreateShortUrlRequest request = request("https://example.com", "custom1");
        when(repository.existsByShortCode("custom1")).thenReturn(false);
        when(currentUser.requireUserId()).thenReturn(7L);
        when(repository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrl result = service.create(request);

        assertThat(result.getShortCode()).isEqualTo("custom1");
        assertThat(result.getOwnerId()).isEqualTo(7L);
        assertThat(result.getStatus()).isEqualTo(ShortUrlStatus.ACTIVE);
        verify(generator, never()).next();
    }

    @Test
    void create_whenCustomCodeExists_shouldThrowAndNotSave() {
        CreateShortUrlRequest request = request("https://example.com", "custom1");
        when(repository.existsByShortCode("custom1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(ShortCodeAlreadyUsed.class);
        verify(repository, never()).save(any());
    }

    @Test
    void create_whenNoCustomCode_shouldGenerateAndSaveUrl() {
        CreateShortUrlRequest request = request("https://example.com", null);
        when(generator.next()).thenReturn("generated");
        when(currentUser.requireUserId()).thenReturn(7L);
        when(repository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrl result = service.create(request);

        assertThat(result.getShortCode()).isEqualTo("generated");
        assertThat(result.getOriginUrl()).isEqualTo("https://example.com");
        assertThat(result.getOwnerId()).isEqualTo(7L);
    }

    @Test
    void getByCodeForRedirect_whenActiveAndNotExpired_shouldReturnUrl() {
        ShortUrl url = url("abc123", ShortUrlStatus.ACTIVE, LocalDateTime.now().plusMinutes(1));
        when(repository.findByShortCode("abc123")).thenReturn(Optional.of(url));

        assertThat(service.getByCodeForRedirect("abc123")).containsSame(url);
    }

    @Test
    void getByCodeForRedirect_whenExpired_shouldThrow() {
        when(repository.findByShortCode("abc123"))
                .thenReturn(Optional.of(url("abc123", ShortUrlStatus.ACTIVE, LocalDateTime.now().minusSeconds(1))));

        assertThatThrownBy(() -> service.getByCodeForRedirect("abc123"))
                .isInstanceOf(UrlExpiredException.class);
    }

    @Test
    void getByCodeForRedirect_whenInactiveOrMissing_shouldThrowNotFound() {
        when(repository.findByShortCode("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByCodeForRedirect("missing"))
                .isInstanceOf(ResourceNotFoundException.class);

        when(repository.findByShortCode("disabled"))
                .thenReturn(Optional.of(url("disabled", ShortUrlStatus.DISABLED, null)));
        assertThatThrownBy(() -> service.getByCodeForRedirect("disabled"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_whenOwnerAndNotDeleted_shouldSoftDeleteAndSave() {
        ShortUrl url = url("abc123", ShortUrlStatus.ACTIVE, null);
        when(currentUser.requireUserId()).thenReturn(7L);
        when(repository.findByShortCode("abc123")).thenReturn(Optional.of(url));
        when(repository.save(url)).thenReturn(url);

        assertThat(service.delete("abc123").getStatus()).isEqualTo(ShortUrlStatus.DELETED);
        verify(repository).save(url);
    }

    @Test
    void delete_whenUserIsNotOwner_shouldThrowUnauthorized() {
        ShortUrl url = url("abc123", ShortUrlStatus.ACTIVE, null);
        when(currentUser.requireUserId()).thenReturn(8L);
        when(repository.findByShortCode("abc123")).thenReturn(Optional.of(url));

        assertThatThrownBy(() -> service.delete("abc123")).isInstanceOf(UnauthorizedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void delete_whenAlreadyDeleted_shouldThrowConflict() {
        ShortUrl url = url("abc123", ShortUrlStatus.DELETED, null);
        when(currentUser.requireUserId()).thenReturn(7L);
        when(repository.findByShortCode("abc123")).thenReturn(Optional.of(url));

        assertThatThrownBy(() -> service.delete("abc123")).isInstanceOf(ConflictException.class);
    }

    @Test
    void update_whenNewCodeIsUsed_shouldThrow() {
        ShortUrl url = url("abc123", ShortUrlStatus.ACTIVE, null);
        UpdateShortUrlRequest request = new UpdateShortUrlRequest();
        request.setShortCode("abc123");
        request.setNewShortCode("new123");
        when(repository.findByShortCode("abc123")).thenReturn(Optional.of(url));
        when(currentUser.requireUserId()).thenReturn(7L);
        when(repository.existsByShortCode("new123")).thenReturn(true);

        assertThatThrownBy(() -> service.update(request)).isInstanceOf(ShortCodeAlreadyUsed.class);
    }

    @Test
    void getMyUrls_shouldQueryCurrentOwnerAndMapResults() {
        ShortUrl url = url("abc123", ShortUrlStatus.ACTIVE, null);
        PageRequest pageRequest = PageRequest.of(0, 10);
        when(currentUser.requireUserId()).thenReturn(7L);
        when(repository.findAllByOwnerId(7L, pageRequest)).thenReturn(new PageImpl<>(List.of(url)));

        assertThat(service.getMyUrls(pageRequest).getContent()).hasSize(1);
        verify(repository).findAllByOwnerId(7L, pageRequest);
    }

    private static CreateShortUrlRequest request(String origin, String customCode) {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginUrl(origin);
        request.setCustomShortCode(customCode);
        return request;
    }

    private static ShortUrl url(String code, ShortUrlStatus status, LocalDateTime expiresAt) {
        return ShortUrl.builder().shortCode(code).originUrl("https://example.com")
                .ownerId(7L).status(status).expiresAt(expiresAt)
                .createdAt(LocalDateTime.now().minusMinutes(1)).updatedAt(LocalDateTime.now().minusMinutes(1)).build();
    }
}
