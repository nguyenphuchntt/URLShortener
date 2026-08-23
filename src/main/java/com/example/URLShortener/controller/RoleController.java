package com.example.URLShortener.controller;

import com.example.URLShortener.dto.request.CreateRoleRequest;
import com.example.URLShortener.dto.response.RoleResponse;
import com.example.URLShortener.entity.Role;
import com.example.URLShortener.mapper.RoleMapper;
import com.example.URLShortener.service.RoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;
    private final RoleMapper roleMapper;

    public RoleController(
            RoleService roleService,
            RoleMapper roleMapper) {
        this.roleService = roleService;
        this.roleMapper = roleMapper;
    }

    @GetMapping("/{name}")
    public ResponseEntity<RoleResponse> getByName(
            @PathVariable
            @NotBlank
            @NotNull
            @Size(min = 2, max = 10)
            @Pattern(regexp = "^[a-zA-Z0-9_-]+$")
            String name) {

        Role role = roleService.getByName(name);
        return ResponseEntity.ok(roleMapper.toResponse(role));
    }

    @PostMapping
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
        Role role = roleService.create(request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(roleMapper.toResponse(role));
    }
}
