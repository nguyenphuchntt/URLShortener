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
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String RAW_PASSWORD = "password";

    /** Hash thật của {@link #RAW_PASSWORD}: login() tự tạo encoder bên trong nên không thể mock. */
    private static final String ENCODED_PASSWORD = PasswordConfig.passwordEncoder().encode(RAW_PASSWORD);

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock RoleRepository roleRepository;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock TokenBlacklistService tokenBlacklistService;

    private AuthServiceImpl service;
    private Role userRole;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userRepository, refreshTokenRepository, roleRepository, jwtTokenProvider, tokenBlacklistService);
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
    void register_whenEmailExists_shouldThrow() {
        RegisterRequest request = registerRequest();
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request)).isInstanceOf(UserAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenDefaultRoleMissing_shouldThrow() {
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(registerRequest()))
                .isInstanceOf(NotImplementedException.class);
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
    void register_shouldPersistRefreshTokenLinkedToNewUser() {
        RegisterRequest request = registerRequest();
        User saved = User.builder().id(1L).username("alice").email("alice@example.com")
                .password("encoded").enabled(true).role(userRole).build();
        LocalDateTime expiry = LocalDateTime.now().plusDays(30);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtTokenProvider.generateAccessToken(1L, "USER")).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("refresh");
        when(jwtTokenProvider.getExpireTime("refresh")).thenReturn(expiry);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.register(request);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("refresh");
        assertThat(captor.getValue().getUser()).isSameAs(saved);
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiry);
        assertThat(captor.getValue().getRevokedAt()).isNull();
    }

    @Test
    void login_whenUserDisabled_shouldThrowUnauthorized() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user("alice", false)));

        assertThatThrownBy(() -> service.login(loginRequest("alice", RAW_PASSWORD)))
                .isInstanceOf(UnauthorizedException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_whenUserMissing_shouldThrowUnauthorized() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(loginRequest("alice", RAW_PASSWORD)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_whenPasswordWrong_shouldThrowUnauthorizedAndNotIssueTokens() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user("alice", true)));

        assertThatThrownBy(() -> service.login(loginRequest("alice", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class);
        verify(jwtTokenProvider, never()).generateAccessToken(anyLong(), anyString());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_whenValidAndNoStoredToken_shouldIssueAndPersistTokens() {
        User user = user("alice", true);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(jwtTokenProvider.generateAccessToken(1L, "USER")).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("refresh");
        when(jwtTokenProvider.getExpireTime("refresh")).thenReturn(LocalDateTime.now().plusDays(30));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JwtResponse response = service.login(loginRequest("alice", RAW_PASSWORD));

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("refresh");
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    @Test
    void login_whenStoredTokenExists_shouldRotateInsteadOfInserting() {
        User user = user("alice", true);
        RefreshToken existing = RefreshToken.builder().id(5L).token("old-refresh").user(user)
                .revokedAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().minusDays(1)).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(jwtTokenProvider.generateAccessToken(1L, "USER")).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("new-refresh");
        when(jwtTokenProvider.getExpireTime("new-refresh")).thenReturn(LocalDateTime.now().plusDays(30));

        JwtResponse response = service.login(loginRequest("alice", RAW_PASSWORD));

        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        assertThat(existing.getToken()).isEqualTo("new-refresh");
        assertThat(existing.getRevokedAt()).isNull();
        verify(refreshTokenRepository).save(existing);
    }

    @Test
    void refresh_whenTokenInvalid_shouldThrowUnauthorized() {
        when(jwtTokenProvider.validateToken("bad")).thenReturn(false);

        assertThatThrownBy(() -> service.refresh("bad")).isInstanceOf(UnauthorizedException.class);
        verify(refreshTokenRepository, never()).findByToken(anyString());
    }

    @Test
    void refresh_whenTokenTypeIsNotRefresh_shouldThrowUnauthorized() {
        when(jwtTokenProvider.validateToken("access")).thenReturn(true);
        when(jwtTokenProvider.getTokenType("access")).thenReturn("access_token");

        assertThatThrownBy(() -> service.refresh("access")).isInstanceOf(UnauthorizedException.class);
        verify(refreshTokenRepository, never()).findByToken(anyString());
    }

    @Test
    void refresh_whenStoredTokenMissing_shouldThrowUnauthorized() {
        when(jwtTokenProvider.validateToken("refresh")).thenReturn(true);
        when(jwtTokenProvider.getTokenType("refresh")).thenReturn("refresh_token");
        when(refreshTokenRepository.findByToken("refresh")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("refresh")).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_whenTokenRevoked_shouldThrowUnauthorized() {
        RefreshToken revoked = RefreshToken.builder().token("refresh").user(user("alice", true))
                .revokedAt(LocalDateTime.now().minusMinutes(1)).build();
        when(jwtTokenProvider.validateToken("refresh")).thenReturn(true);
        when(jwtTokenProvider.getTokenType("refresh")).thenReturn("refresh_token");
        when(refreshTokenRepository.findByToken("refresh")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.refresh("refresh")).isInstanceOf(UnauthorizedException.class);
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void refresh_whenUserMissing_shouldThrowUnauthorized() {
        RefreshToken stored = RefreshToken.builder().token("refresh").user(user("alice", true)).build();
        when(jwtTokenProvider.validateToken("refresh")).thenReturn(true);
        when(jwtTokenProvider.getTokenType("refresh")).thenReturn("refresh_token");
        when(refreshTokenRepository.findByToken("refresh")).thenReturn(Optional.of(stored));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("refresh")).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_whenUserDisabled_shouldThrowUnauthorized() {
        RefreshToken stored = RefreshToken.builder().token("refresh").user(user("alice", true)).build();
        when(jwtTokenProvider.validateToken("refresh")).thenReturn(true);
        when(jwtTokenProvider.getTokenType("refresh")).thenReturn("refresh_token");
        when(refreshTokenRepository.findByToken("refresh")).thenReturn(Optional.of(stored));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user("alice", false)));

        assertThatThrownBy(() -> service.refresh("refresh")).isInstanceOf(UnauthorizedException.class);
        verify(jwtTokenProvider, never()).generateAccessToken(anyLong(), anyString());
    }

    @Test
    void refresh_whenValid_shouldReturnNewAccessTokenAndKeepRefreshToken() {
        User user = user("alice", true);
        RefreshToken stored = RefreshToken.builder().token("refresh").user(user).build();
        when(jwtTokenProvider.validateToken("refresh")).thenReturn(true);
        when(jwtTokenProvider.getTokenType("refresh")).thenReturn("refresh_token");
        when(refreshTokenRepository.findByToken("refresh")).thenReturn(Optional.of(stored));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(1L, "USER")).thenReturn("new-access");

        JwtResponse response = service.refresh("refresh");

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logout_whenTokenExists_shouldRevokeAndReturnTrue() {
        RefreshToken token = RefreshToken.builder().token("refresh").build();
        when(refreshTokenRepository.findByToken("refresh")).thenReturn(Optional.of(token));
        when(jwtTokenProvider.validateToken("access")).thenReturn(true);
        when(jwtTokenProvider.getJti("access")).thenReturn("jti-1");
        when(jwtTokenProvider.getRemainingTtlSeconds("access")).thenReturn(600L);

        assertThat(service.logout("refresh", "access")).isTrue();
        assertThat(token.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(token);
        verify(tokenBlacklistService).revoke("jti-1", 600L);
    }

    @Test
    void logout_withoutAccessToken_shouldStillRevokeRefreshToken() {
        RefreshToken token = RefreshToken.builder().token("refresh").build();
        when(refreshTokenRepository.findByToken("refresh")).thenReturn(Optional.of(token));

        assertThat(service.logout("refresh", null)).isTrue();
        assertThat(token.getRevokedAt()).isNotNull();
        verify(tokenBlacklistService, never()).revoke(any(), anyLong());
    }

    @Test
    void logout_whenTokenMissing_shouldThrowUnauthorized() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.logout("missing", null)).isInstanceOf(UnauthorizedException.class);
        verify(refreshTokenRepository, never()).save(any());
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
        return User.builder().id(1L).username(username).email(username + "@example.com")
                .password(ENCODED_PASSWORD).enabled(enabled)
                .role(Role.builder().name("USER").build()).build();
    }
}
