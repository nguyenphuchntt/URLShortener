package com.example.URLShortener.controller;

import com.example.URLShortener.dto.request.ChangePasswordRequest;
import com.example.URLShortener.dto.request.UpdateUserRequest;
import com.example.URLShortener.dto.response.UserProfileResponse;
import com.example.URLShortener.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile() { return ResponseEntity.ok(userService.getMyProfile()); }

    @PatchMapping
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateMyProfile(request));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changeMyPassword(request);
        return ResponseEntity.noContent().build();
    }
}
