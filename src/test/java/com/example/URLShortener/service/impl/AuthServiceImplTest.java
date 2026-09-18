package com.example.URLShortener.service.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock RoleRepository roleRepository;
    @Mock JwtTokenProvider jwtTokenProvider;

    private AuthServiceImpl service;
    private Role userRole;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userRepository, refreshTokenRepository, roleRepository, jwtTokenProvider);
        userRole = Role.builder().name("USER").build();
    }

    @Test
    void register_whenUsernameExists_shouldThrow() {
        RegisterRequest request = registerRequest();
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request)).isInstanceOf(UserAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenValid_shouldSaveUserAndToken() {
        RegisterRequest request = registerRequest();
        User saved = User.builder().id(1L).username("alice").email("alice@example.com")
                .password("encoded").enabled(true).role(userRole).build();
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtTokenProvider.generateAccessToken(1L, "USER")).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("refresh");
        when(jwtTokenProvider.getExpireTime("refresh")).thenReturn(LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response = service.register(request);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_whenUserDisabled_shouldThrowUnauthorized() {
        User user = user("alice", false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(loginRequest("alice", "password")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_whenUserMissing_shouldThrowUnauthorized() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(loginRequest("alice", "password")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_whenTokenInvalid_shouldThrowUnauthorized() {
        when(jwtTokenProvider.validateToken("bad")).thenReturn(false);

        assertThatThrownBy(() -> service.refresh("bad")).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logout_whenTokenExists_shouldRevokeAndReturnTrue() {
        RefreshToken token = RefreshToken.builder().token("refresh").build();
        when(refreshTokenRepository.findByToken("refresh")).thenReturn(Optional.of(token));

        assertThat(service.logout("refresh")).isTrue();
        assertThat(token.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(token);
    }

    private static RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("password");
        return request;
    }

    private static LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private static User user(String username, boolean enabled) {
        return User.builder().id(1L).username(username).password("$2a$10$7EqJtq98hPqEX7fNZaFWoO5oJfQ0d4kM1x5vXqfJ9X5Jm9J0X4Y7K")
                .enabled(enabled).role(Role.builder().name("USER").build()).build();
    }
}
