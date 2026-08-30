package com.example.URLShortener.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LogoutRequest {

    @Pattern(regexp = "^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*$", message = "Invalid token format")
    String refreshToken;
}
