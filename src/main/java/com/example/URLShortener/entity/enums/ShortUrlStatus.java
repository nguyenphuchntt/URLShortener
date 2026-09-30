package com.example.URLShortener.entity.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status of a short link")
public enum ShortUrlStatus {

    @Schema(description = "Link is active, `/r/{shortCode}` redirects normally")
    ACTIVE,

    @Schema(description = "Link is temporarily disabled, `/r/{shortCode}` does not redirect")
    DISABLED,

    @Schema(description = "Link has been soft-deleted, `/r/{shortCode}` returns 404")
    DELETED
}

