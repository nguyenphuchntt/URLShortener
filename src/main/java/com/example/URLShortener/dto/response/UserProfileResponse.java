package com.example.URLShortener.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "Profile of the currently signed-in user")
@Data
@Builder
public class UserProfileResponse {

    @Schema(description = "User ID", example = "7")
    private Long id;

    @Schema(description = "Username", example = "nguyenphuc01")
    private String username;

    @Schema(description = "Email", example = "phuc@example.com")
    private String email;

    @Schema(description = "Whether the account is active or disabled", example = "true")
    private boolean enabled;

    @Schema(description = "Role name", example = "USER")
    private String role;

    @Schema(description = "Account creation time", example = "2026-08-05T09:55:00")
    private java.time.LocalDateTime createdAt;
}
