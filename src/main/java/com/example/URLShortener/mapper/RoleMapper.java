package com.example.URLShortener.mapper;

import com.example.URLShortener.dto.response.RoleResponse;
import com.example.URLShortener.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleResponse toResponse(Role role);
}
