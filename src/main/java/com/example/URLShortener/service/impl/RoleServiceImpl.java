package com.example.URLShortener.service.impl;

import com.example.URLShortener.entity.Role;
import com.example.URLShortener.entity.enums.ErrorCode;
import com.example.URLShortener.exception.ResourceNotFoundException;
import com.example.URLShortener.exception.RoleAlreadyExistsException;
import com.example.URLShortener.repository.RoleRepository;
import com.example.URLShortener.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Role getByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.URL_NOT_FOUND, "Role not found: " + name));
    }

    @Override
    @Transactional
    public Role create(String name) {
        if (roleRepository.existsByName(name)) {
            throw new RoleAlreadyExistsException(
                    String.format("Role '%s' already exists", name)
            );
        }
        Role role = Role.builder()
                .name(name)
                .build();

        role =  roleRepository.save(role);
        logger.info("New role created: {}", role.getName());
        return role;
    }
}