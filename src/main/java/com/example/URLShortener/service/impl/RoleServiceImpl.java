package com.example.URLShortener.service.impl;

import com.example.URLShortener.entity.Role;
import com.example.URLShortener.entity.enums.ErrorCode;
import com.example.URLShortener.exception.ResourceNotFoundException;
import com.example.URLShortener.repository.RoleRepository;
import com.example.URLShortener.service.RoleService;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Role getByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ROLE_NOT_FOUND, "Role not found: " + name));
    }

    @Override
    public Role create(String name) {
        return roleRepository.save(Role.builder().name(name).build());
    }
}