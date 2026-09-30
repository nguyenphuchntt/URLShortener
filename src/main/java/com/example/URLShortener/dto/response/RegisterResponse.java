package com.example.URLShortener.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "Registration result: newly created user information with login tokens")
@Data
@Builder
public class RegisterResponse {

    @Schema(description = "Username vừa tạo", example = "nguyenphuc01")
    private String username;

    @Schema(description = "Id của user vừa tạo", example = "7")
    private Long userId;

    @Schema(description = "Access token, valid for 15 minutes",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuZ3V5ZW5waHVjMDEifQ.abc123")
    private String accessToken;

    @Schema(description = "Refresh token, valid for 30 days",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuZ3V5ZW5waHVjMDEifQ.def456")
    private String refreshToken;
}
