package com.example.URLShortener.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Schema(description = "Login credentials")
@Data
public class LoginRequest {

    @Schema(description = "Registered username", example = "nguyenphuc01")
    @NotBlank
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{7,29}$", message = "Invalid username format")
    private String username;

    @Schema(description = "Password", example = "P@ssw0rd123")
    @NotBlank
    private String password;
}
