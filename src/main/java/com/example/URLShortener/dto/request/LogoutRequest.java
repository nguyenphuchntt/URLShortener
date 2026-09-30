package com.example.URLShortener.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Schema(description = """
        Logout request. A refresh token is required for revocation; the access token (if it should also be blacklisted)
        is sent in the `Authorization` header, not in this body.
        """)
@Data
public class LogoutRequest {

    @Schema(description = "Refresh token to revoke.",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuZ3V5ZW5waHVjMDEifQ.abc123")
    @Pattern(regexp = "^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*$", message = "Invalid token format")
    String refreshToken;
}
