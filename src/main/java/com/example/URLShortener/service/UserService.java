package com.example.URLShortener.service;

import com.example.URLShortener.dto.request.ChangePasswordRequest;
import com.example.URLShortener.dto.request.UpdateUserRequest;
import com.example.URLShortener.dto.response.UserProfileResponse;
import com.example.URLShortener.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    User getById(Long id);
    List<User> getAll();
    Page<User> getAll(Pageable pageable);
    Boolean deleteById(Long id);
    UserProfileResponse getMyProfile();
    UserProfileResponse updateMyProfile(UpdateUserRequest request);
    void changeMyPassword(ChangePasswordRequest request);
    UserProfileResponse setEnabled(Long id, boolean enabled);
}
