package com.example.URLShortener.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{7,29}$", message = "Invalid username format")
    private String username;

    private String password;
}
