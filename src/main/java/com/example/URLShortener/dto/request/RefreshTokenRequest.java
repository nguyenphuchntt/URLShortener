package com.example.URLShortener.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Schema(description = "Request to refresh the access token. The refresh token is sent in the body, not the header.")
@Data
public class RefreshTokenRequest {

    @Schema(description = "Refresh token received from `/login`, `/register`, or the previous `/refresh` request.",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuZ3V5ZW5waHVjMDEifQ.abc123")
    @Pattern(regexp = "^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*$", message = "Invalid token format")
    private String refreshToken;

}
