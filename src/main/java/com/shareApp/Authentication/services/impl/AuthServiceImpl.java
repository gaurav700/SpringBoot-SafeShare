package com.shareApp.Authentication.services.impl;


import com.shareApp.Authentication.dto.SignUpDTO;
import com.shareApp.Authentication.dto.UserDTO;
import com.shareApp.Authentication.entities.User;
import com.shareApp.Authentication.entities.enums.Role;
import com.shareApp.Utils.exceptions.ResourceNotFoundException;
import com.shareApp.Utils.exceptions.RuntimeConflictException;
import com.shareApp.Authentication.repositories.UserRepository;
import com.shareApp.Utils.security.JWTService;
import com.shareApp.Authentication.services.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    @Override
    public String[] login(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        User user = (User) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new String[]{accessToken, refreshToken};
    }

    @Override
    @Transactional
    public UserDTO signUp(SignUpDTO signUpDTO) {

        User u = userRepository.findByEmail(signUpDTO.getEmail()).orElse(null);
        if(u!=null){
            throw new RuntimeConflictException("Cannot signUp with this email, mail is already exists "+ signUpDTO.getEmail());
        }

        User user = modelMapper.map(signUpDTO, User.class);
        user.setRoles(Set.of(Role.USER));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User save = userRepository.save(user);


        return modelMapper.map(save, UserDTO.class);
    }


    @Override
    public String refreshToken(String refreshToken) {
        Long userId = jwtService.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(String.valueOf(userId)).orElseThrow(()->
                new ResourceNotFoundException("User not found with this id :"+userId));
        return jwtService.generateAccessToken(user);
    }
}
