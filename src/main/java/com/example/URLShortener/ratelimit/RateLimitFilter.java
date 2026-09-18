package com.example.URLShortener.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@AllArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
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
                && "/api/auth/login".equals(path)) {
            return "login";
        }

        if ("POST".equals(method)
                && "/api/auth/register".equals(path)) {
            return "register";
        }

        if ("POST".equals(method)
                && "/api/auth/forgot-password".equals(path)) {
            return "forgot-password";
        }

        if ("POST".equals(method)
                && "/api/auth/resend-verification".equals(path)) {
            return "resend-verification";
        }

        if ("POST".equals(method)
                && "/api/auth/otp-verify".equals(path)) {
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
