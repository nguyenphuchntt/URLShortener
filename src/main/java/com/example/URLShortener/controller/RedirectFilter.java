package com.example.URLShortener.controller;

import com.example.URLShortener.cache.UrlCacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RedirectFilter extends OncePerRequestFilter {

    private final UrlCacheService urlCacheService;
    private final String redirectPath;

    public RedirectFilter(
            UrlCacheService urlCacheService,
            @Value("${short-code.redirect-path}") String redirectPath) {
        this.urlCacheService = urlCacheService;
        this.redirectPath = redirectPath;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String shortCode = extractShortCodeFromRequest(request);
        if (shortCode == null) {
            filterChain.doFilter(request, response);
            return;
        }
        // Cache HIT
        var cached = urlCacheService.get(shortCode);
        if (cached.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        var v = cached.get();
        if (v.isExpired()) {
            urlCacheService.evict(shortCode);
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.FOUND.value());
        response.setHeader(HttpHeaders.LOCATION, v.originUrl());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return;
    }

    private String extractShortCodeFromRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith(redirectPath)) {
            return null;
        }
        String code = path.substring(redirectPath.length());
        if (code.isEmpty() || code.contains("/")) return null;
        if (!code.matches("^[a-zA-Z0-9]{1,16}$")) return null;
        return code;
    }
}
