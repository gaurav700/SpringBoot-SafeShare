package com.shareApp.services;


import com.shareApp.dto.SignUpDTO;
import com.shareApp.dto.UserDTO;

public interface AuthService {
    String[] login(String email, String password);

    UserDTO signUp(SignUpDTO signUpDTO);

    String refreshToken(String refreshToken);
}
