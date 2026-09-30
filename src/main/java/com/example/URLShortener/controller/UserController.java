package com.example.URLShortener.controller;

import com.example.URLShortener.dto.request.ChangePasswordRequest;
import com.example.URLShortener.dto.request.UpdateUserRequest;
import com.example.URLShortener.dto.response.UserProfileResponse;
import com.example.URLShortener.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "Your own profile")
@RestController
@RequestMapping(value = "/api/v1/users/me", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService userService;

    @Operation(summary = "View your profile",
            description = "Returns the signed-in user's information, derived from the access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile details"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile() { return ResponseEntity.ok(userService.getMyProfile()); }

    @Operation(summary = "Update your profile",
            description = """
                    Changes `username` and/or `email`. Fields left empty (null) keep their previous values.
                    The new `username` must not already be used by someone else → `409` on a duplicate.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The profile after the update"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    @PatchMapping
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateMyProfile(request));
    }

    @Operation(summary = "Change password",
            description = """
                    Requires the correct `currentPassword`. `newPassword` must be at least 8 characters.
                    After the change, tokens that are currently valid are **not** revoked.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed successfully, no body"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    })
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changeMyPassword(request);
        return ResponseEntity.noContent().build();
    }
}
