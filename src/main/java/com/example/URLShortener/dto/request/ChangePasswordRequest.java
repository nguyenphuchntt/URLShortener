package com.example.URLShortener.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "Request to change your own password")
@Data
public class ChangePasswordRequest {

    @Schema(description = "Current password. An incorrect value results in `401` `UNAUTHORIZED`.",
            example = "P@ssw0rd123")
    @NotBlank
    private String currentPassword;

    @Schema(description = "New password, 8–255 characters. Special-character rules are not required.",
            example = "N3wP@ssw0rd456")
    @NotBlank
    @Size(min = 8, max = 255)
    private String newPassword;
}
