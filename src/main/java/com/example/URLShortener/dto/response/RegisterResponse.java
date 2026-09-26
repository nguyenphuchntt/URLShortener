package com.example.URLShortener.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponse {
    private String username;
    private Long userId;

    private String accessToken;
    private String refreshToken;
}
