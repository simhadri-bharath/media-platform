package com.bharath.media_backend.controller;

import com.bharath.media_backend.api.dto.LoginRequest;
import com.bharath.media_backend.api.dto.SignupRequest;
import com.bharath.media_backend.api.dto.TokenResponse;
import com.bharath.media_backend.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public String signup(@RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
