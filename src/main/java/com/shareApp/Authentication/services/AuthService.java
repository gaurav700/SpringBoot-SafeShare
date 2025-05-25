package com.shareApp.Authentication.services;


import com.shareApp.Authentication.dto.CheckUserDTO;
import com.shareApp.Authentication.dto.SignUpDTO;
import com.shareApp.Authentication.dto.UserDTO;

public interface AuthService {
    String[] login(String email, String password);

    UserDTO signUp(SignUpDTO signUpDTO);

    String refreshToken(String refreshToken);

    Boolean checkUser(CheckUserDTO checkUserDTO);
}
