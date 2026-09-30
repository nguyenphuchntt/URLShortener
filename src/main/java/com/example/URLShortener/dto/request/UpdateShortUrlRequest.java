package com.example.URLShortener.dto.request;

import com.example.URLShortener.entity.enums.ShortUrlStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Schema(description = """
        Request to update a short link. Send only fields to change; empty (null) fields keep their
        previous values. The link is identified by its current `shortCode`, not by ID.
        """)
@Data
public class UpdateShortUrlRequest {

    @Schema(description = "Current short code of the link to update. Required to locate the link.",
            example = "u3Kf9a")
    @Size(min = 6, max = 16)
    @Pattern(regexp = "^[a-zA-Z0-9]+$")
    private String shortCode;

    @Schema(description = "New original URL. Leave empty to keep the current value.",
            example = "https://example.com/new-destination")
    @URL
    private String originUrl;

    @Schema(description = """
            Change to a different short code. Letters and digits only, 6–16 characters.
            An existing code results in `409` `SHORT_CODE_TAKEN`.
            """,
            example = "newLink9")
    @Size(min = 6, max = 16)
    @Pattern(regexp = "^[a-zA-Z0-9]+$")
    private String newShortCode;

    @Schema(description = "New link status. Leave empty to keep the current value.",
            example = "DISABLED")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ShortUrlStatus status;

    @Schema(description = "New expiration time, which must be in the future. Send `null` to remove the expiration.",
            example = "2027-06-30T23:59:59")
    @Nullable
    @Future
    private LocalDateTime expiresAt;
}
