package com.example.URLShortener.security.jwt;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "a-very-long-test-secret-key-for-jwt-123456";

    @Test
    void accessToken_shouldContainExpectedClaims() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, "test-issuer", 60_000, 120_000);

        String token = provider.generateAccessToken(7L, "ROLE_USER");

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo(7L);
        assertThat(provider.getRole(token)).isEqualTo("ROLE_USER");
        assertThat(provider.getTokenType(token)).isEqualTo("access_token");
        assertThat(provider.getAuthorities(token)).extracting(Object::toString).contains("ROLE_USER");
    }

    @Test
    void refreshToken_shouldContainRefreshTypeAndNoRole() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, "test-issuer", 60_000, 120_000);

        String token = provider.generateRefreshToken(9L);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo(9L);
        assertThat(provider.getTokenType(token)).isEqualTo("refresh_token");
        assertThat(provider.getRole(token)).isNull();
    }

    @Test
    void validateToken_whenMalformedOrSignedByOtherProvider_shouldReturnFalse() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, "test-issuer", 60_000, 120_000);
        JwtTokenProvider otherProvider = new JwtTokenProvider("another-long-test-secret-key-for-jwt-123456", "test-issuer", 60_000, 120_000);

        assertThat(provider.validateToken("not-a-token")).isFalse();
        assertThat(provider.validateToken(otherProvider.generateAccessToken(1L, "ROLE_USER"))).isFalse();
    }

    @Test
    void validateToken_whenExpired_shouldReturnFalse() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, "test-issuer", -1, -1);

        assertThat(provider.validateToken(provider.generateAccessToken(1L, "ROLE_USER"))).isFalse();
    }

    @Test
    void getExpireTime_shouldReturnExpirationInTheFutureForValidToken() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, "test-issuer", 60_000, 120_000);
        String token = provider.generateAccessToken(1L, "ROLE_USER");

        assertThat(provider.getExpireTime(token).toInstant(ZoneOffset.UTC))
                .isAfter(java.time.Instant.now());
    }
}
