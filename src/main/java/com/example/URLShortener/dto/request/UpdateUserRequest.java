package com.example.URLShortener.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Size(min = 3, max = 255)
    private String username;

    @Email
    @Size(max = 100)
    private String email;
}
