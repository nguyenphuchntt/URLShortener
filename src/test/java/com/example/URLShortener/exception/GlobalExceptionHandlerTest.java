package com.example.URLShortener.exception;

import com.example.URLShortener.dto.response.ErrorResponse;
import com.example.URLShortener.entity.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * Verifies the exception → HTTP status/ErrorCode contract that clients depend on.
 * Instantiates the advice directly (no Spring context).
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String PATH = "/api/v1/test";

    @Mock HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        lenient().when(request.getRequestURI()).thenReturn(PATH);
    }

    @Test
    void handleAppException_resourceNotFound_shouldMapTo404() {
        ResponseEntity<ErrorResponse> res =
                handler.handleAppException(new ResourceNotFoundException("missing"), request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody().getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertBodyEnvelope(res.getBody(), HttpStatus.NOT_FOUND);
    }

    @Test
    void handleShortCodeUsed_shouldMapTo409WithShortCodeTaken() {
        ResponseEntity<ErrorResponse> res =
                handler.handleShortCodeUsed(new ShortCodeAlreadyUsed("taken"), request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().getErrorCode()).isEqualTo(ErrorCode.SHORT_CODE_TAKEN);
    }

    @Test
    void handleExpiredShortCode_shouldMapToItsOwnStatusAndCode() {
        ResponseEntity<ErrorResponse> res =
                handler.handleExpiredShortCode(new UrlExpiredException("expired"), request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(res.getBody().getErrorCode()).isEqualTo(ErrorCode.URL_EXPIRED);
    }

    @Test
    void handleUnauthorized_shouldMapTo401() {
        ResponseEntity<ErrorResponse> res =
                handler.handleUnauthorized(new UnauthorizedException("nope"), request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody().getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void handleAppException_forbiddenException_shouldMapTo403() {
        ResponseEntity<ErrorResponse> res =
                handler.handleAppException(new ForbiddenException("not allowed"), request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody().getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertBodyEnvelope(res.getBody(), HttpStatus.FORBIDDEN);
    }

    @Test
    void handleForbidden_shouldMapTo403() {
        HttpClientErrorException.Forbidden ex =
                (HttpClientErrorException.Forbidden) HttpClientErrorException.create(
                        HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY, null, null);

        ResponseEntity<ErrorResponse> res = handler.handleForbidden(ex, request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody().getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void handleRoleAlreadyExists_shouldMapTo409() {
        ResponseEntity<ErrorResponse> res =
                handler.handleRoleAlreadyExists(new RoleAlreadyExistsException("dup"), request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().getErrorCode()).isEqualTo(ErrorCode.ROLE_ALREADY_EXISTS);
    }

    @Test
    void handleUsernameNotFound_shouldMapTo401() {
        ResponseEntity<ErrorResponse> res =
                handler.handleUsernameNotFound(new UsernameNotFoundException("no user"), request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody().getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void handleMethodNotAllowed_shouldMapTo405() {
        ResponseEntity<ErrorResponse> res = handler.handleMethodNotAllowed(
                new HttpRequestMethodNotSupportedException("PUT"), request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(res.getBody().getErrorCode()).isEqualTo(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @Test
    void handleUnexpected_shouldMapTo500WithoutLeakingMessage() {
        ResponseEntity<ErrorResponse> res =
                handler.handleUnexpected(new IllegalStateException("secret internals"), request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody().getErrorCode()).isEqualTo(ErrorCode.UNEXPECTED_ERROR);
        assertThat(res.getBody().getErrorMessage()).isEqualTo("Unexpected error");
    }

    private static void assertBodyEnvelope(ErrorResponse body, HttpStatus expectedStatus) {
        assertThat(body.getStatus()).isEqualTo(expectedStatus.value());
        assertThat(body.getPath()).isEqualTo(PATH);
        assertThat(body.getTime()).isNotNull();
    }
}
