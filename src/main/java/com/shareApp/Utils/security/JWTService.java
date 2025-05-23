package com.shareApp.Utils.security;

import com.shareApp.Authentication.entities.User;
import com.shareApp.Utils.exceptions.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@Slf4j
public class JWTService {
    @Value("${jwt.secretKey}")
    private String jwtSecretKey;

    private SecretKey getSecretKey(){
        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user){
        // Store user ID as string directly, no need to convert if it's already string
        String userId = user.getId(); // Assuming getId() returns String for MongoDB

        return Jwts.builder()
                .subject(userId) // Store as string directly
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+ 1000*60*10))
                .signWith(getSecretKey())
                .compact();
    }

    public String generateRefreshToken(User user){
        String userId = user.getId(); // Assuming getId() returns String for MongoDB

        return Jwts.builder()
                .subject(userId) // Store as string directly
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+ 1000L * 60*60*24*30*6))
                .signWith(getSecretKey())
                .compact();
    }

    // Updated method to return String instead of Long
    public String getUserIdFromToken(String token){
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject(); // Return as String, don't parse to Long
    }

    public String validateTokenAndGetUserId(String authToken) {
        try {
            log.debug("Validating token: {}", authToken != null ? "Present" : "Null");

            // Remove "Bearer " prefix if present
            String token = authToken.startsWith("Bearer ") ?
                    authToken.substring(7) : authToken;

            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            log.debug("Extracted user ID from token: {}", userId);

            return userId; // Return as String
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid or expired token");
        }
    }
}