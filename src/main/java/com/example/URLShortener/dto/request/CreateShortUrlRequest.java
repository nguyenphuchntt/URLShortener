package com.example.URLShortener.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Schema(description = "Request to create a short link")
@Data
public class CreateShortUrlRequest {

    @Schema(description = "Original URL to shorten. Required and must be a valid URL with a scheme.",
            example = "https://example.com/very/long/path?utm_source=newsletter")
    @NotBlank
    @URL
    private String originUrl;

    @Schema(description = """
            Optional short code. Leave empty to generate one automatically from the counter and Base62.
            Letters and digits only, 6–16 characters. If the code is already in use, the result is `409` `SHORT_CODE_TAKEN`.
            """,
            example = "myLink1")
    @Size(min = 6, max = 16)
    @Pattern(regexp = "^[a-zA-Z0-9]+$")
    private String customShortCode;

    @Schema(description = "Expiration time, which must be in the future. Empty means the link never expires.",
            example = "2027-01-01T00:00:00")
    @Future
    private LocalDateTime expiresAt;
}
