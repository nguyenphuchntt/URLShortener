package com.example.URLShortener.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "Profile update request. Empty (null) fields keep their previous values.")
@Data
public class UpdateUserRequest {

    @Schema(description = "New username, 3–255 characters. A duplicate results in `409`.",
            example = "nguyenphuc02")
    @Size(min = 3, max = 255)
    private String username;

    @Schema(description = "New email address, up to 100 characters.",
            example = "phuc.moi@example.com")
    @Email
    @Size(max = 100)
    private String email;
}
