package com.example.URLShortener.service.impl;

import com.example.URLShortener.dto.response.AdminUserResponse;
import com.example.URLShortener.dto.response.UserProfileResponse;
import com.example.URLShortener.entity.Role;
import com.example.URLShortener.entity.User;
import com.example.URLShortener.exception.ResourceNotFoundException;
import com.example.URLShortener.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock UserService userService;

    private AdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminServiceImpl(userService);
    }

    @Test
    void getAllUsers_shouldMapPageToAdminResponses() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        when(userService.getAll(pageRequest))
                .thenReturn(new PageImpl<>(List.of(user(1L, "alice", true, "ADMIN"))));

        Page<AdminUserResponse> result = service.getAllUsers(pageRequest);

        assertThat(result.getContent()).singleElement().satisfies(response -> {
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getUsername()).isEqualTo("alice");
            assertThat(response.getEmail()).isEqualTo("alice@example.com");
            assertThat(response.isEnabled()).isTrue();
            assertThat(response.getRole()).isEqualTo("ADMIN");
            assertThat(response.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void getAllUsers_whenUserHasNoRole_shouldMapRoleAsNull() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        when(userService.getAll(pageRequest))
                .thenReturn(new PageImpl<>(List.of(user(1L, "alice", true, null))));

        assertThat(service.getAllUsers(pageRequest).getContent())
                .singleElement()
                .extracting(AdminUserResponse::getRole)
                .isNull();
    }

    @Test
    void getAllUsers_whenNoUsers_shouldReturnEmptyPage() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        when(userService.getAll(pageRequest)).thenReturn(new PageImpl<>(List.of()));

        assertThat(service.getAllUsers(pageRequest).getContent()).isEmpty();
    }

    @Test
    void adminResponse_shouldNotExposePassword() {
        assertThat(Arrays.stream(AdminUserResponse.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("password");
    }

    @Test
    void getUserById_shouldDelegateToUserServiceAndMapResult() {
        when(userService.getById(1L)).thenReturn(user(1L, "alice", true, "USER"));

        AdminUserResponse result = service.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getRole()).isEqualTo("USER");
        verify(userService).getById(1L);
    }

    @Test
    void getUserById_whenMissing_shouldPropagateResourceNotFound() {
        when(userService.getById(99L)).thenThrow(new ResourceNotFoundException("User not found"));

        assertThatThrownBy(() -> service.getUserById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateUserStatus_shouldReturnResponseWithNewStatus() {
        when(userService.setEnabled(3L, false)).thenReturn(profile(3L, "bob", false, "USER"));

        AdminUserResponse result = service.updateUserStatus(3L, false);

        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getUsername()).isEqualTo("bob");
        assertThat(result.isEnabled()).isFalse();
        assertThat(result.getRole()).isEqualTo("USER");
        verify(userService).setEnabled(3L, false);
    }

    @Test
    void deleteUser_shouldDelegateToUserService() {
        service.deleteUser(3L);

        verify(userService).deleteById(3L);
    }

    private static User user(Long id, String username, boolean enabled, String roleName) {
        return User.builder().id(id).username(username).email(username + "@example.com")
                .password("encoded").enabled(enabled)
                .role(roleName == null ? null : Role.builder().id(1).name(roleName).build())
                .createdAt(LocalDateTime.now()).build();
    }

    private static UserProfileResponse profile(Long id, String username, boolean enabled, String roleName) {
        return UserProfileResponse.builder().id(id).username(username).email(username + "@example.com")
                .enabled(enabled).role(roleName).createdAt(LocalDateTime.now()).build();
    }
}
