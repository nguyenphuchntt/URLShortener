package com.example.URLShortener.service.impl;

import com.example.URLShortener.dto.request.ChangePasswordRequest;
import com.example.URLShortener.dto.request.UpdateUserRequest;
import com.example.URLShortener.dto.response.UserProfileResponse;
import com.example.URLShortener.entity.User;
import com.example.URLShortener.exception.ConflictException;
import com.example.URLShortener.exception.ResourceNotFoundException;
import com.example.URLShortener.exception.UnauthorizedException;
import com.example.URLShortener.repository.UserRepository;
import com.example.URLShortener.security.CurrentUser;
import com.example.URLShortener.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public List<User> getAll() { return userRepository.findAll(); }

    @Override
    public Page<User> getAll(Pageable pageable) { return userRepository.findAll(pageable); }

    @Override
    @Transactional
    public Boolean deleteById(Long id) {
        User user = getById(id);
        user.setEnabled(false);
        userRepository.save(user);
        return true;
    }

    @Override
    public UserProfileResponse getMyProfile() { return toResponse(getById(currentUser.requireUserId())); }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(UpdateUserRequest request) {
        User user = getById(currentUser.requireUserId());
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) throw new ConflictException("Username already used");
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) throw new ConflictException("Email already used");
            user.setEmail(request.getEmail());
        }
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changeMyPassword(ChangePasswordRequest request) {
        User user = getById(currentUser.requireUserId());
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is invalid");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserProfileResponse setEnabled(Long id, boolean enabled) {
        User user = getById(id);
        user.setEnabled(enabled);
        return toResponse(userRepository.save(user));
    }

    private UserProfileResponse toResponse(User user) {
        return UserProfileResponse.builder().id(user.getId()).username(user.getUsername()).email(user.getEmail())
                .enabled(user.isEnabled()).role(user.getRole() == null ? null : user.getRole().getName())
                .createdAt(user.getCreatedAt()).build();
    }
}
