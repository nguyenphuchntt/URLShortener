package com.example.URLShortener.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;

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
            response.setContentType("application/json");
            response.getWriter().write("""
                    {
                        "error": "Too many requests",
                        "message": "Rate limit exceeded"
                    }
                    """);
            return;
        }
        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(result.getRemainingTokens())
        );

        filterChain.doFilter(request, response);
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
