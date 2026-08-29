package com.example.URLShortener.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final String issuer;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${security.jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor( // secret + algorithm
                secret.getBytes(StandardCharsets.UTF_8)
        );
        this.issuer = issuer;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(Long userId, String role) {
        return generateToken(
                userId,
                role,
                accessTokenExpiration,
                "access_token"
        );
    }

    public String generateRefreshToken(Long userId) {
        return generateToken(
                userId,
                null,
                refreshTokenExpiration,
                "refresh_token"
        );
    }

    private String generateToken(
            Long userId,
            String role,
            long expiration,
            String tokenType
    ) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expiration);
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(issuer)
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(secretKey);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Collection<? extends GrantedAuthority> getAuthorities(String token) {
        String role = getRole(token);
        return List.of(new SimpleGrantedAuthority(role));
    }

    public Long getUserId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }
}
