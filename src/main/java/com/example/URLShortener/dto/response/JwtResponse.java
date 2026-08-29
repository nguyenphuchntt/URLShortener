package com.example.URLShortener.dto.response;

import java.time.LocalDateTime;

public class JwtResponse {

    private String accessToken;
    private String refreshToken;

    private String tokenType;
    private LocalDateTime expiresAt;

}
