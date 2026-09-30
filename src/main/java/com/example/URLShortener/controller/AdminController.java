package com.example.URLShortener.controller;

import com.example.URLShortener.dto.request.UpdateUserStatusRequest;
import com.example.URLShortener.dto.response.AdminUserResponse;
import com.example.URLShortener.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin", description = "User management")
@RestController
@RequestMapping(value = "/api/v1/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    private final AdminService adminService;

    @Operation(
            summary = "List all users",
            description = """
                    **Requires the `ADMIN` authority.** Returns a page of all users in the system.
                    Supports `page` (zero-based), `size` (default 20, max 100) and `sort`,
                    for example `sort=createdAt,desc`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of users"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    })
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponse>> getUsers(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @Operation(
            summary = "Get a user",
            description = "**Requires the `ADMIN` authority.** Unknown id → `404` (`USER_NOT_FOUND`)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User details"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserResponse> getUser(
            @Parameter(description = "User id", example = "7")
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @Operation(
            summary = "Enable/disable a user",
            description = """
                    **Requires the `ADMIN` authority.** Set `enabled = false` to lock the account: a locked user
                    can no longer sign in, but their data is kept intact. Set `enabled = true` to unlock it.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The user after the status change"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<AdminUserResponse> updateStatus(
            @Parameter(description = "User id", example = "7")
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, request.getEnabled()));
    }

    @Operation(
            summary = "Delete a user",
            description = "**Requires the `ADMIN` authority.** Deletes the user by id. This action cannot be undone."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted successfully, no body"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User id", example = "7")
            @PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
