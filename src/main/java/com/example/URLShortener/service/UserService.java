package com.example.URLShortener.service;

import com.example.URLShortener.entity.User;

import java.util.List;

public interface UserService {

    User getById(Long id);

    List<User> getAll();

    void deleteById(Long id);
}
