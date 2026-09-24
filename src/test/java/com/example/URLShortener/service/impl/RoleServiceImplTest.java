package com.example.URLShortener.service.impl;

import com.example.URLShortener.entity.Role;
import com.example.URLShortener.entity.enums.ErrorCode;
import com.example.URLShortener.exception.ResourceNotFoundException;
import com.example.URLShortener.exception.RoleAlreadyExistsException;
import com.example.URLShortener.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock RoleRepository roleRepository;

    private RoleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoleServiceImpl(roleRepository);
    }

    @Test
    void getByName_whenRoleExists_shouldReturnRole() {
        Role role = Role.builder().id(2).name("ADMIN").build();
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));

        assertThat(service.getByName("ADMIN")).isSameAs(role);
    }

    @Test
    void getByName_whenRoleMissing_shouldThrowResourceNotFound() {
        when(roleRepository.findByName("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByName("MISSING"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.URL_NOT_FOUND);
    }

    @Test
    void create_whenNameExists_shouldThrowRoleAlreadyExistsAndNotSave() {
        when(roleRepository.existsByName("ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> service.create("ADMIN"))
                .isInstanceOf(RoleAlreadyExistsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_ALREADY_EXISTS);
        verify(roleRepository, never()).save(any());
    }

    @Test
    void create_whenNew_shouldSaveRoleWithGivenName() {
        when(roleRepository.existsByName("MANAGER")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create("MANAGER");

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("MANAGER");
        assertThat(captor.getValue().getId()).isNull();
    }

    @Test
    void create_shouldReturnPersistedEntity() {
        Role persisted = Role.builder().id(3).name("MANAGER").build();
        when(roleRepository.existsByName("MANAGER")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(persisted);

        Role result = service.create("MANAGER");

        assertThat(result.getId()).isEqualTo(3);
        assertThat(result.getName()).isEqualTo("MANAGER");
    }
}
