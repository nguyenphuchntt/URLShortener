package com.example.URLShortener.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class JwtResponse {

    private String accessToken;
    private String refreshToken;
}
