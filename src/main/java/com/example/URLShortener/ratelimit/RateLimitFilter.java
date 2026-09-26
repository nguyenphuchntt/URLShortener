package com.example.URLShortener.ratelimit;

import com.example.URLShortener.dto.response.ErrorResponse;
import com.example.URLShortener.entity.enums.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String FALLBACK_BODY =
            "{\"status\":429,\"errorMessage\":\"Rate limit exceeded\",\"errorCode\":\"RATE_LIMIT_EXCEEDED\"}";

    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // Bypass the entire filter when ratelimit.enabled=false (e.g. perf testing).
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String endpoint = resolveEndpoint(request);
        String identifier = resolveIdentifier(request);
        RateLimitResult result = rateLimitService.tryConsume(
                identifier,
                endpoint
        );
        if (!result.isAllowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(
                    "Retry-After",
                    String.valueOf(result.getRetryAfterSeconds())
            );
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(errorBody(request));
            return;
        }
        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(result.getRemainingTokens())
        );

        filterChain.doFilter(request, response);
    }

    private String errorBody(HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .errorMessage("Rate limit exceeded")
                .errorCode(ErrorCode.RATE_LIMIT_EXCEEDED)
                .path(request.getRequestURI())
                .build();
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            log.warn("Failed to serialize rate limit response body", e);
            return FALLBACK_BODY;
        }
    }

    private String resolveEndpoint(
            HttpServletRequest request
    ) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("POST".equals(method)
                && "/api/v1/auth/login".equals(path)) {
            return "login";
        }

        if ("POST".equals(method)
                && "/api/v1/auth/register".equals(path)) {
            return "register";
        }

        if ("POST".equals(method)
                && "/api/v1/auth/forgot-password".equals(path)) {
            return "forgot-password";
        }

        if ("POST".equals(method)
                && "/api/v1/auth/resend-verification".equals(path)) {
            return "resend-verification";
        }

        if ("POST".equals(method)
                && "/api/v1/auth/otp-verify".equals(path)) {
            return "otp-verify";
        }

        return null;
    }

    private String resolveIdentifier(
            HttpServletRequest request
    ) {
        return "ip:" + request.getRemoteAddr();
    }
}
