package com.example.URLShortener.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserResponse {
    private Long id;
    private String username;
    private String email;
    private boolean enabled;
    private String role;
    private java.time.LocalDateTime createdAt;
}
