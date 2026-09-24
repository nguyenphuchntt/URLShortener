package com.example.URLShortener.service.impl;

import com.example.URLShortener.config.PasswordConfig;
import com.example.URLShortener.dto.request.LoginRequest;
import com.example.URLShortener.dto.request.RegisterRequest;
import com.example.URLShortener.dto.response.JwtResponse;
import com.example.URLShortener.dto.response.RegisterResponse;
import com.example.URLShortener.entity.RefreshToken;
import com.example.URLShortener.entity.Role;
import com.example.URLShortener.entity.User;
import com.example.URLShortener.exception.UnauthorizedException;
import com.example.URLShortener.exception.UserAlreadyExistsException;
import com.example.URLShortener.repository.RefreshTokenRepository;
import com.example.URLShortener.repository.RoleRepository;
import com.example.URLShortener.repository.UserRepository;
import com.example.URLShortener.security.jwt.JwtTokenProvider;
import com.example.URLShortener.security.jwt.TokenBlacklistService;
import com.example.URLShortener.service.AuthService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Registration attempt for username {}", request.getUsername());
        Role userRole = roleRepository.findByName("USER").orElseThrow(() -> new NotImplementedException("User role not found"));
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already existed");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already used by another account");
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(PasswordConfig.passwordEncoder().encode(request.getPassword()))
                .enabled(true)
                .role(userRole)
                .createdAt(LocalDateTime.now())
                .build();

        newUser = userRepository.save(newUser);

        String accessToken = jwtTokenProvider.generateAccessToken(
            newUser.getId(), newUser.getRole().getName()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(
            newUser.getId()
        );

        RefreshToken token = RefreshToken.builder()
                .token(refreshToken)
                .user(newUser)
                .createdAt(LocalDateTime.now())
                .expiresAt(jwtTokenProvider.getExpireTime(refreshToken))
                .build();

        token = refreshTokenRepository.save(token);

        log.info("Created an user with username {}", newUser.getUsername());

        return RegisterResponse.builder()
                .username(newUser.getUsername())
                .userId(newUser.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    @Transactional
    public JwtResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        if (!PasswordConfig.passwordEncoder().matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid username or password");
        }
        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getRole().getName()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId()
        );

        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserId(user.getId());
        RefreshToken token;
        if (existingToken.isPresent()) {
            token = existingToken.get();
            token.setToken(refreshToken);
            token.setExpiresAt(jwtTokenProvider.getExpireTime(refreshToken));
            token.setRevokedAt(null);
            token.setCreatedAt(LocalDateTime.now());
        } else {
            token = RefreshToken.builder()
                    .token(refreshToken)
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(jwtTokenProvider.getExpireTime(refreshToken))
                    .build();
        }
        refreshTokenRepository.save(token);

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public JwtResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        if (!"refresh_token".equals(jwtTokenProvider.getTokenType(refreshToken))) {
            throw new UnauthorizedException("Invalid token type");
        }
        RefreshToken refToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (refToken.getRevokedAt() != null) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        Long userId = refToken.getUser().getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }
        String accessToken = jwtTokenProvider.generateAccessToken(
                userId,
                user.getRole().getName()
        );
        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    @Transactional
    public boolean logout(String refreshToken, String accessToken) {
        // Revoke the access token
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            tokenBlacklistService.revoke(
                    jwtTokenProvider.getJti(accessToken),
                    jwtTokenProvider.getRemainingTtlSeconds(accessToken)
            );
        }
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("User already logout"));
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
        return true;
    }
}
