package com.bharath.media_backend.controller;

import com.bharath.media_backend.api.dto.*;
import com.bharath.media_backend.domain.AdminUser;
import com.bharath.media_backend.exception.InvalidCredentialsException;
import com.bharath.media_backend.exception.UserAlreadyExistsException;
import com.bharath.media_backend.repo.AdminUserRepository;
import com.bharath.media_backend.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AdminUserRepository adminRepo;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(AdminUserRepository adminRepo, JwtService jwtService) {
        this.adminRepo = adminRepo;
        this.jwtService = jwtService;
    }

    @PostMapping("/signup")
    public String signup(@RequestBody SignupRequest request) {
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

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        AdminUser user = adminRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!encoder.matches(request.getPassword(), user.getHashedPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new TokenResponse(token);
    }
}
