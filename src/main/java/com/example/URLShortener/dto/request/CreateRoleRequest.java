package com.example.URLShortener.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "Request to create a new role (ADMIN only)")
@Data
public class CreateRoleRequest {

    @Schema(description = "New role name, up to 255 characters. Duplicate names result in `409` `ROLE_ALREADY_EXISTS`.",
            example = "MODERATOR")
    @NotBlank
    @Size(max = 255)
    private String name;
}
