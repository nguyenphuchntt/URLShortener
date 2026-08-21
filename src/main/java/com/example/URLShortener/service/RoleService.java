package com.example.URLShortener.service;

import com.example.URLShortener.entity.Role;

public interface RoleService {

    Role getByName(String name);

    Role create(String name);
}
