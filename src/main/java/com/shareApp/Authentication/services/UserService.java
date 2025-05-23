package com.shareApp.Authentication.services;

import com.shareApp.Authentication.entities.User;

public interface UserService {
    User getUserById(Long userId);
}
