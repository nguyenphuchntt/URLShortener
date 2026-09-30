package com.example.URLShortener.entity.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stable error code that clients use for programmatic handling (the `errorCode` field of ErrorResponse)")
public enum ErrorCode {

    @Schema(description = "Unexpected server-side error (HTTP 500)")
    UNEXPECTED_ERROR,

    @Schema(description = "Feature not implemented (HTTP 501)")
    NOT_IMPLEMENTED,

    @Schema(description = "The short link has expired (HTTP 410)")
    URL_EXPIRED,

    @Schema(description = "HTTP method not supported by the endpoint (HTTP 405)")
    METHOD_NOT_ALLOWED,

    @Schema(description = "The requested resource was not found (HTTP 404)")
    RESOURCE_NOT_FOUND,

    @Schema(description = "No short link found for the given short code (HTTP 404)")
    URL_NOT_FOUND,

    @Schema(description = "The URL already exists in the system (HTTP 409)")
    URL_ALREADY_EXISTS,

    @Schema(description = "The submitted URL has an invalid format (HTTP 400)")
    INVALID_URL_FORMAT,

    @Schema(description = "The requested short code is already in use (HTTP 409)")
    SHORT_CODE_TAKEN,

    @Schema(description = "No more short codes available to allocate (HTTP 500)")
    KEY_POOL_EXHAUSTED,

    @Schema(description = "User not found (HTTP 404)")
    USER_NOT_FOUND,

    @Schema(description = "Not authenticated, wrong credentials, or the token is invalid/expired/revoked (HTTP 401)")
    UNAUTHORIZED,

    @Schema(description = "Authenticated but lacking permission to perform the operation (HTTP 403)")
    FORBIDDEN,

    @Schema(description = "Too many requests within the allowed time window (HTTP 429)")
    RATE_LIMIT_EXCEEDED,

    @Schema(description = "The submitted data or parameters are invalid (HTTP 400)")
    INVALID_PARAMETER_FORMAT,

    @Schema(description = "The role to be created already exists (HTTP 409)")
    ROLE_ALREADY_EXISTS
}