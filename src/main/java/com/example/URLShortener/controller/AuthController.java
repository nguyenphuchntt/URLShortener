package com.example.URLShortener.controller;

import com.example.URLShortener.dto.request.LoginRequest;
import com.example.URLShortener.dto.request.LogoutRequest;
import com.example.URLShortener.dto.request.RefreshTokenRequest;
import com.example.URLShortener.dto.request.RegisterRequest;
import com.example.URLShortener.dto.response.JwtResponse;
import com.example.URLShortener.dto.response.RegisterResponse;
import com.example.URLShortener.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Register, sign in, refresh, logout")
@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AuthService authService;

    public AuthController (
            AuthService authService
    ) {
        this.authService = authService;
    }

    @Operation(
            summary = "Register an account",
            description = """
                    Creates a new user with the default `USER` role. Returns the token pair right away,
                    so the client does not need to call `/login` afterwards. Returns `409` when the
                    username or email already exists.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registered successfully"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(
            summary = "Sign in",
            description = """
                    Exchanges `username` + `password` for an `accessToken` (valid for 15 minutes) and a
                    `refreshToken` (valid for 30 days). Wrong credentials → `401`, `errorCode = UNAUTHORIZED`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signed in successfully"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    })
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.login(request));
    }

    @Operation(
            summary = "Refresh the access token",
            description = """
                    Takes the `refreshToken` **in the body** (not the `Authorization` header).
                    The old refresh token is revoked and a new token pair is returned. An invalid,
                    already used or revoked token → `401`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A new token pair is issued"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    })
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.refresh(request.getRefreshToken()));
    }

    @Operation(
            summary = "Log out",
            description = """
                    Revokes the `refreshToken` in the body. Send the header `Authorization: Bearer <accessToken>`
                    to also blacklist the current access token; it is optional, and when omitted only the
                    refresh token is revoked. A refresh token that does not exist or is already revoked → `401`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logged out successfully, no body"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request,
            @Parameter(description = "The current access token, as `Bearer <token>`. Optional.",
                    example = "Bearer eyJhbGciOiJIUzI1NiJ9...")
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String accessToken = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : null;
        authService.logout(request.getRefreshToken(), accessToken);
        return ResponseEntity.noContent().build();
    }
}
