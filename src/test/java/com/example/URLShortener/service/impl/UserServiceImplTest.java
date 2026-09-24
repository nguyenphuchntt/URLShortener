package com.example.URLShortener.service.impl;

import com.example.URLShortener.dto.request.ChangePasswordRequest;
import com.example.URLShortener.dto.request.UpdateUserRequest;
import com.example.URLShortener.dto.response.UserProfileResponse;
import com.example.URLShortener.entity.Role;
import com.example.URLShortener.entity.User;
import com.example.URLShortener.exception.ConflictException;
import com.example.URLShortener.exception.ResourceNotFoundException;
import com.example.URLShortener.exception.UnauthorizedException;
import com.example.URLShortener.repository.UserRepository;
import com.example.URLShortener.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock CurrentUser currentUser;
    @Mock PasswordEncoder passwordEncoder;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, currentUser, passwordEncoder);
    }

    @Test
    void getById_whenUserMissing_shouldThrowResourceNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMyProfile_shouldReturnProfileOfCurrentUser() {
        when(currentUser.requireUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "alice", true)));

        UserProfileResponse result = service.getMyProfile();

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getRole()).isEqualTo("USER");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void getMyProfile_whenNotAuthenticated_shouldThrowUnauthorized() {
        when(currentUser.requireUserId()).thenThrow(new UnauthorizedException("User is not authenticated"));

        assertThatThrownBy(() -> service.getMyProfile()).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void profileResponse_shouldNotExposePassword() {
        assertThat(Arrays.stream(UserProfileResponse.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("password");
    }

    @Test
    void updateMyProfile_whenUsernameTaken_shouldThrowConflictAndNotSave() {
        when(currentUser.requireUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "alice", true)));
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        assertThatThrownBy(() -> service.updateMyProfile(updateRequest("bob", null)))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMyProfile_whenEmailTaken_shouldThrowConflictAndNotSave() {
        when(currentUser.requireUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "alice", true)));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.updateMyProfile(updateRequest("alice", "taken@example.com")))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMyProfile_whenValuesUnchanged_shouldNotQueryUniqueness() {
        User user = user(7L, "alice", true);
        when(currentUser.requireUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserProfileResponse result = service.updateMyProfile(updateRequest("alice", "alice@example.com"));

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void updateMyProfile_whenValuesAvailable_shouldPersistNewValues() {
        User user = user(7L, "alice", true);
        when(currentUser.requireUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("bobby")).thenReturn(false);
        when(userRepository.existsByEmail("bobby@example.com")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        UserProfileResponse result = service.updateMyProfile(updateRequest("bobby", "bobby@example.com"));

        assertThat(result.getUsername()).isEqualTo("bobby");
        assertThat(result.getEmail()).isEqualTo("bobby@example.com");
        verify(userRepository).save(user);
    }

    @Test
    void updateMyProfile_whenRequestIsEmpty_shouldKeepCurrentValues() {
        User user = user(7L, "alice", true);
        when(currentUser.requireUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserProfileResponse result = service.updateMyProfile(new UpdateUserRequest());

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void changeMyPassword_whenCurrentPasswordInvalid_shouldThrowUnauthorizedAndNotSave() {
        User user = user(7L, "alice", true);
        when(currentUser.requireUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-current")).thenReturn(false);

        assertThatThrownBy(() -> service.changeMyPassword(passwordRequest("wrong", "new-password")))
                .isInstanceOf(UnauthorizedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeMyPassword_whenValid_shouldStoreEncodedPasswordOnly() {
        User user = user(7L, "alice", true);
        when(currentUser.requireUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "encoded-current")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");

        service.changeMyPassword(passwordRequest("current", "new-password"));

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        assertThat(user.getPassword()).isNotEqualTo("new-password");
        verify(userRepository).save(user);
    }

    @Test
    void setEnabled_shouldUpdateFlagAndReturnProfile() {
        User user = user(3L, "bob", true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserProfileResponse result = service.setEnabled(3L, false);

        assertThat(result.isEnabled()).isFalse();
        assertThat(user.isEnabled()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void deleteById_shouldDisableUserAndReturnTrue() {
        User user = user(3L, "bob", true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        assertThat(service.deleteById(3L)).isTrue();
        assertThat(user.isEnabled()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void deleteById_whenUserMissing_shouldThrowResourceNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getAll_withPageable_shouldQueryRepository() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        when(userRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(user(1L, "alice", true))));

        assertThat(service.getAll(pageRequest).getContent()).hasSize(1);
        verify(userRepository).findAll(pageRequest);
    }

    @Test
    void getAll_withoutPageable_shouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user(1L, "alice", true), user(2L, "bob", true)));

        assertThat(service.getAll()).hasSize(2);
    }

    private static UpdateUserRequest updateRequest(String username, String email) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername(username);
        request.setEmail(email);
        return request;
    }

    private static ChangePasswordRequest passwordRequest(String current, String replacement) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(current);
        request.setNewPassword(replacement);
        return request;
    }

    private static User user(Long id, String username, boolean enabled) {
        return User.builder().id(id).username(username).email(username + "@example.com")
                .password("encoded-current").enabled(enabled)
                .role(Role.builder().id(1).name("USER").build())
                .createdAt(LocalDateTime.now()).build();
    }
}
