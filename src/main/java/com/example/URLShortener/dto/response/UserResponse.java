package com.example.URLShortener.dto.response;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserResponse {

    private String username;

    @Email
    private String email;

    private boolean enabled;
}
