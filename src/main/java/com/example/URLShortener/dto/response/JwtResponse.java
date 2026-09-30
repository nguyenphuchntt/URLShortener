package com.example.URLShortener.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "Token pair returned after login or token refresh")
@Data
@Builder
public class JwtResponse {

    @Schema(description = "Access token for the `Authorization: Bearer <token>` header; valid for 15 minutes",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuZ3V5ZW5waHVjMDEifQ.abc123")
    private String accessToken;

    @Schema(description = "Refresh token used to obtain a new access token; valid for 30 days",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuZ3V5ZW5waHVjMDEifQ.def456")
    private String refreshToken;
}
