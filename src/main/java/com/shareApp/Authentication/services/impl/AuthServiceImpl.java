// Updated AuthServiceImpl.java - MongoDB Implementation
package com.shareApp.Authentication.services.impl;

import com.shareApp.Authentication.dto.CheckUserDTO;
import com.shareApp.Authentication.dto.SignUpDTO;
import com.shareApp.Authentication.dto.UserDTO;
import com.shareApp.Authentication.entities.OTP;
import com.shareApp.Authentication.entities.User;
import com.shareApp.Authentication.entities.enums.Role;
import com.shareApp.Authentication.repositories.UserRepository;
import com.shareApp.Authentication.services.OTPService;
import com.shareApp.Utils.exceptions.ResourceNotFoundException;
import com.shareApp.Utils.exceptions.RuntimeConflictException;
import com.shareApp.Utils.security.JWTService;
import com.shareApp.Authentication.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final OTPService otpService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    @Override
    public String[] login(String email, String password) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            log.info("Attempting login for email: {}", normalizedEmail);

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, password)
            );

            User user = (User) authentication.getPrincipal();
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            log.info("Login successful for user: {}", user.getEmail());
            return new String[]{accessToken, refreshToken};

        } catch (AuthenticationException e) {
            log.warn("Login failed for email: {} - {}", email, e.getMessage());
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Override
    public UserDTO signUp(SignUpDTO signUpDTO) {
        String email = signUpDTO.getEmail().toLowerCase().trim();

        log.info("Attempting signup for email: {}", email);

        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeConflictException("User already exists with email: " + email);
        }

        // Check if OTP was verified (additional security check)
        if (!otpService.isOTPVerified(email, OTP.OTPType.SIGNUP)) {
            throw new RuntimeConflictException("Email verification required. Please verify your email first.");
        }

        try {
            // Create new user
            User user = new User();
            user.setName(signUpDTO.getName().trim());
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(signUpDTO.getPassword()));
            user.setRoles(Set.of(Role.USER));
            user.setEnabled(true);
            user.setEmailVerified(true); // Since OTP was verified
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            User savedUser = userRepository.save(user);

            // Clean up verified OTP
            otpService.cleanupVerifiedOTP(email, OTP.OTPType.SIGNUP);

            log.info("User created successfully: {}", savedUser.getEmail());

            return modelMapper.map(savedUser, UserDTO.class);

        } catch (Exception e) {
            log.error("Failed to create user for email: {}", email, e);
            throw new RuntimeException("Failed to create user account");
        }
    }

    @Override
    public String refreshToken(String refreshToken) {
        try {
            String userId = jwtService.getUserIdFromToken(refreshToken);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            return jwtService.generateAccessToken(user);

        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new RuntimeException("Invalid refresh token");
        }
    }

     @Override
    public Boolean checkUser(CheckUserDTO checkUserDTO) {
        String email = checkUserDTO.getEmail().toLowerCase().trim();
        boolean exists = userRepository.existsByEmail(email);

        log.info("User check for email: {} - exists: {}", email, exists);
        return exists;
    }

}