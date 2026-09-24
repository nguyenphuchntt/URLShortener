package com.example.URLShortener.service;

import com.example.URLShortener.dto.request.LoginRequest;
import com.example.URLShortener.dto.request.RegisterRequest;
import com.example.URLShortener.dto.response.JwtResponse;
import com.example.URLShortener.dto.response.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);
    JwtResponse login(LoginRequest request);
    JwtResponse refresh(String refreshToken);
    boolean logout(String refreshToken, String accessToken);
}
