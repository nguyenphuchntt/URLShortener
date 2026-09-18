package com.example.URLShortener.service;

import com.example.URLShortener.dto.response.AdminUserResponse;
import com.example.URLShortener.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {
    Page<AdminUserResponse> getAllUsers(Pageable pageable);
    AdminUserResponse getUserById(Long id);
    AdminUserResponse updateUserStatus(Long id, boolean enabled);
    void deleteUser(Long id);
}
