package com.example.URLShortener.controller;

import com.example.URLShortener.dto.request.CreateRoleRequest;
import com.example.URLShortener.dto.response.RoleResponse;
import com.example.URLShortener.entity.Role;
import com.example.URLShortener.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/{name}")
    public ResponseEntity<RoleResponse> getByName(@PathVariable String name) {
        Role role = roleService.getByName(name);
        return ResponseEntity.ok(toResponse(role));
    }

    @PostMapping
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
        Role role = roleService.create(request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(role));
    }

    private RoleResponse toResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .build();
    }
}
