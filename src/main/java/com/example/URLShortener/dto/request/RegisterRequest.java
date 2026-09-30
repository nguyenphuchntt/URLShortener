package com.example.URLShortener.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "Request to register a new account")
@Data
public class RegisterRequest {

    @Schema(description = "Username, 8–30 characters, starting with a letter and containing only letters, digits, or `_`.",
            example = "nguyenphuc01")
    @NotBlank
    @Size(min = 6, max = 50)
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{7,29}$")
    private String username;

    @Schema(description = "Email address; must have a valid format and not be already in use.",
            example = "phuc@example.com")
    @NotBlank
    @Email
    private String email;

    @Schema(description = """
            Password: at least 8 characters, including at least one uppercase letter, one lowercase letter, one digit, and
            one special character from `@ $ ! % * ? &`.
            """,
            example = "P@ssw0rd123")
    @NotBlank
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
    message = "Password must be at least 8 characters long and include at least one uppercase letter, one lowercase letter, one number, and one special character.")
    private String password;
}
