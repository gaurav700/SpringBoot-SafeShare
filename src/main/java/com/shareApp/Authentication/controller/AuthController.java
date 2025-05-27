// Updated AuthController.java - Using existing ApiResponse classes
package com.shareApp.Authentication.controller;

import com.shareApp.Authentication.dto.*;
import com.shareApp.Authentication.entities.OTP;
import com.shareApp.Authentication.entities.User;
import com.shareApp.Authentication.repositories.UserRepository;
import com.shareApp.Authentication.services.AuthService;
import com.shareApp.Authentication.services.OTPService;
import com.shareApp.Utils.advices.ApiError;
import com.shareApp.Utils.advices.ApiResponse;
import com.shareApp.Utils.exceptions.ResourceNotFoundException;
import com.shareApp.Utils.security.JWTService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final OTPService otpService;
    private final JWTService jwtService;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @PostMapping("/check-user")
    public ResponseEntity<Boolean> checkUser(@Valid @RequestBody CheckUserDTO checkUserDTO) {
        boolean exists = authService.checkUser(checkUserDTO);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/get-user-detail")
    public ResponseEntity<ApiResponse<UserDTO>> checkUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Invalid Authorization header");
            }

            String token = authHeader.substring(7);
            String userId = jwtService.getUserIdFromToken(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

            UserDTO userDTO = modelMapper.map(user, UserDTO.class);
            ApiResponse<UserDTO> response = new ApiResponse<>(userDTO);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("User check failed", e);
            ApiError apiError = ApiError.builder()
                    .status(HttpStatus.UNAUTHORIZED)
                    .message(e.getMessage())
                    .build();
            ApiResponse<UserDTO> response = new ApiResponse<>(apiError);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }


    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendOTP(@Valid @RequestBody SendOTPRequestDTO request) {
        OTPResponseDTO result = otpService.sendOTP(request.getEmail(), request.getType());

        if (result.isSuccess()) {
            ApiResponse<Map<String, String>> response = new ApiResponse<>(
                    Map.of("message", result.getMessage())
            );
            return ResponseEntity.ok(response);
        } else {
            ApiError apiError = ApiError.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(result.getMessage())
                    .build();
            ApiResponse<Map<String, String>> response = new ApiResponse<>(apiError);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOTP(@Valid @RequestBody VerifyOTPRequestDTO request) {
        OTPResponseDTO result = otpService.verifyOTP(request.getEmail(), request.getOtp());

        if (result.isSuccess()) {
            ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                    Map.of(
                            "message", result.getMessage(),
                            "verified", true
                    )
            );
            return ResponseEntity.ok(response);
        } else {
            ApiError apiError = ApiError.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(result.getMessage())
                    .build();
            ApiResponse<Map<String, Object>> response = new ApiResponse<>(apiError);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/signUp")
    public ResponseEntity<ApiResponse<UserDTO>> signUp(@Valid @RequestBody SignUpDTO signUpDTO) {
        try {
            // Check if OTP was verified for this email
            boolean otpVerified = otpService.isOTPVerified(signUpDTO.getEmail(), OTP.OTPType.SIGNUP);
            if (!otpVerified) {
                ApiError apiError = ApiError.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("Email verification required. Please verify your email first.")
                        .build();
                ApiResponse<UserDTO> response = new ApiResponse<>(apiError);
                return ResponseEntity.badRequest().body(response);
            }

            UserDTO user = authService.signUp(signUpDTO);
            ApiResponse<UserDTO> response = new ApiResponse<>(user);
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (Exception e) {
            log.error("Signup failed for email: {}", signUpDTO.getEmail(), e);
            ApiError apiError = ApiError.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(e.getMessage())
                    .build();
            ApiResponse<UserDTO> response = new ApiResponse<>(apiError);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO loginRequestDTO,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {

        try {
            String[] tokens = authService.login(loginRequestDTO.getEmail(), loginRequestDTO.getPassword());

            Cookie cookie = new Cookie("refreshToken", tokens[1]);
            cookie.setHttpOnly(true);
            cookie.setSecure(true); // Set to true in production
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            httpServletResponse.addCookie(cookie);

            LoginResponseDTO loginResponse = new LoginResponseDTO(tokens[0]);
            ApiResponse<LoginResponseDTO> response = new ApiResponse<>(loginResponse);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Login failed for email: {}", loginRequestDTO.getEmail(), e);
            ApiError apiError = ApiError.builder()
                    .status(HttpStatus.UNAUTHORIZED)
                    .message("Invalid email or password")
                    .build();
            ApiResponse<LoginResponseDTO> response = new ApiResponse<>(apiError);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> refresh(HttpServletRequest request) {
        try {
            String refreshToken = Arrays.stream(request.getCookies())
                    .filter(cookie -> "refreshToken".equals(cookie.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElseThrow(() -> new AuthenticationServiceException("Refresh token not found"));

            String accessToken = authService.refreshToken(refreshToken);

            LoginResponseDTO loginResponse = new LoginResponseDTO(accessToken);
            ApiResponse<LoginResponseDTO> response = new ApiResponse<>(loginResponse);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            ApiError apiError = ApiError.builder()
                    .status(HttpStatus.UNAUTHORIZED)
                    .message("Invalid or expired refresh token")
                    .build();
            ApiResponse<LoginResponseDTO> response = new ApiResponse<>(apiError);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(HttpServletResponse response) {
        // Clear refresh token cookie
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        ApiResponse<Map<String, String>> apiResponse = new ApiResponse<>(
                Map.of("message", "Logged out successfully")
        );
        return ResponseEntity.ok(apiResponse);
    }
}