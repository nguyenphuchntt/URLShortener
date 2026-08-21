package com.example.URLShortener.dto.response;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserResponse {

    String username;

    @Email
    String email;

    boolean enabled;
}
