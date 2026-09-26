package com.example.URLShortener.controller;

import com.example.URLShortener.dto.request.LoginRequest;
import com.example.URLShortener.dto.request.LogoutRequest;
import com.example.URLShortener.dto.request.RefreshTokenRequest;
import com.example.URLShortener.dto.request.RegisterRequest;
import com.example.URLShortener.dto.response.ErrorResponse;
import com.example.URLShortener.dto.response.JwtResponse;
import com.example.URLShortener.dto.response.LogoutResponse;
import com.example.URLShortener.dto.response.RegisterResponse;
import com.example.URLShortener.entity.enums.ErrorCode;
import com.example.URLShortener.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController (
            AuthService authService
    ) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @Valid @RequestBody LogoutRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String accessToken = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : null;
        if (authService.logout(request.getRefreshToken(), accessToken)) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(new LogoutResponse());
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.builder()
                            .time(LocalDateTime.now())
                            .errorCode(ErrorCode.UNEXPECTED_ERROR)
                            .errorMessage("Failed to logout")
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build()
            );
        }
    }
}
