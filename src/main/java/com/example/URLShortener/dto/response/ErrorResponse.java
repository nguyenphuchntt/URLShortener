package com.example.URLShortener.dto.response;

import com.example.URLShortener.entity.enums.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = """
        Shared error body for **all** error responses: 400, 401, 403, 404, 409, 410, 429, and 500.
        Even `429` rate-limit responses (created by a filter rather than the exception handler) use this shape.
        """)
@Data
@Builder
@AllArgsConstructor
public class ErrorResponse {

    @Schema(description = "Time when the server generated the error", example = "2026-09-26T15:57:00.123")
    private LocalDateTime time;

    @Schema(description = "HTTP status code, repeated in the body for convenience", example = "404")
    private int status;

    @Schema(description = "Human-readable error description", example = "Short code not found")
    private String errorMessage;

    @Schema(description = "Stable error code for client-side logic; always a value from the ErrorCode enum",
            example = "RESOURCE_NOT_FOUND")
    private ErrorCode errorCode;

    @Schema(description = "Path of the request that caused the error", example = "/api/v1/urls/u3Kf9a")
    private String path;
}
