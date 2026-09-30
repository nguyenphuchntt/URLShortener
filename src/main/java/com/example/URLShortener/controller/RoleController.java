package com.example.URLShortener.controller;

import com.example.URLShortener.dto.request.CreateRoleRequest;
import com.example.URLShortener.dto.response.RoleResponse;
import com.example.URLShortener.entity.Role;
import com.example.URLShortener.mapper.RoleMapper;
import com.example.URLShortener.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin", description = "User management")
@Validated
@RestController
@RequestMapping(value = "/api/v1/roles", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final RoleService roleService;
    private final RoleMapper roleMapper;

    public RoleController(
            RoleService roleService,
            RoleMapper roleMapper) {
        this.roleService = roleService;
        this.roleMapper = roleMapper;
    }

    @Operation(
            summary = "Get a role by name",
            description = """
                    **Requires the `ADMIN` authority** (enforced by `@PreAuthorize`, not shown in the UI).
                    The role name must be 2–10 characters, containing only letters, digits, `_` and `-`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role details"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    @GetMapping("/{name}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<RoleResponse> getByName(
            @Parameter(description = "Role name", example = "ADMIN")
            @PathVariable
            @NotBlank
            @NotNull
            @Size(min = 2, max = 10)
            @Pattern(regexp = "^[a-zA-Z0-9_-]+$")
            String name) {

        Role role = roleService.getByName(name);
        return ResponseEntity.ok(roleMapper.toResponse(role));
    }

    @Operation(
            summary = "Create a new role",
            description = """
                    **Requires the `ADMIN` authority.** Creates a role with a `name` that does not exist yet;
                    a duplicate name → `409` (`ROLE_ALREADY_EXISTS`). The name can be at most 255 characters.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Role created successfully"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
        Role role = roleService.create(request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(roleMapper.toResponse(role));
    }
}
