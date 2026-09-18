package com.example.URLShortener.service.impl;

import com.example.URLShortener.dto.response.AdminUserResponse;
import com.example.URLShortener.entity.User;
import com.example.URLShortener.service.AdminService;
import com.example.URLShortener.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final UserService userService;

    @Override
    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {
        return userService.getAll(pageable).map(this::toResponse);
    }

    @Override
    public AdminUserResponse getUserById(Long id) { return toResponse(userService.getById(id)); }

    @Override
    public AdminUserResponse updateUserStatus(Long id, boolean enabled) {
        var profile = userService.setEnabled(id, enabled);
        return AdminUserResponse.builder().id(profile.getId()).username(profile.getUsername()).email(profile.getEmail())
                .enabled(profile.isEnabled()).role(profile.getRole()).createdAt(profile.getCreatedAt()).build();
    }

    @Override
    public void deleteUser(Long id) { userService.deleteById(id); }

    private AdminUserResponse toResponse(User user) {
        return AdminUserResponse.builder().id(user.getId()).username(user.getUsername()).email(user.getEmail())
                .enabled(user.isEnabled()).role(user.getRole() == null ? null : user.getRole().getName())
                .createdAt(user.getCreatedAt()).build();
    }
}
