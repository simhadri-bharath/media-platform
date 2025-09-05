package com.bharath.media_backend.service;

import com.bharath.media_backend.api.dto.LoginRequest;
import com.bharath.media_backend.api.dto.SignupRequest;
import com.bharath.media_backend.api.dto.TokenResponse;
import com.bharath.media_backend.domain.AdminUser;
import com.bharath.media_backend.exception.InvalidCredentialsException;
import com.bharath.media_backend.exception.UserAlreadyExistsException;
import com.bharath.media_backend.repo.AdminUserRepository;
import com.bharath.media_backend.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final AdminUserRepository adminRepo;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(AdminUserRepository adminRepo, JwtService jwtService) {
        this.adminRepo = adminRepo;
        this.jwtService = jwtService;
    }

    public String signup(SignupRequest request) {
        if (adminRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        AdminUser user = AdminUser.builder()
                .email(request.getEmail())
                .hashedPassword(encoder.encode(request.getPassword()))
                .role("ADMIN")
                .createdAt(Instant.now())
                .build();

        adminRepo.save(user);
        return "Admin registered successfully";
    }

    public TokenResponse login(LoginRequest request) {
        AdminUser user = adminRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!encoder.matches(request.getPassword(), user.getHashedPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new TokenResponse(token);
    }
}
