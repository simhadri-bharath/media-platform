package com.bharath.media_backend.controller;

import com.bharath.media_backend.api.dto.LoginRequest;
import com.bharath.media_backend.api.dto.SignupRequest;
import com.bharath.media_backend.api.dto.TokenResponse;
import com.bharath.media_backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSignup() {
        SignupRequest request = new SignupRequest();
        request.setEmail("admin@example.com");
        request.setPassword("password123");

        when(authService.signup(request)).thenReturn("Admin registered successfully");

        String response = authController.signup(request);

        assertEquals("Admin registered successfully", response);
        verify(authService, times(1)).signup(request);
    }

    @Test
    void testLogin() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@example.com");
        request.setPassword("password123");

        TokenResponse tokenResponse = new TokenResponse("mock-jwt-token");
        when(authService.login(request)).thenReturn(tokenResponse);

        TokenResponse response = authController.login(request);

        assertEquals("mock-jwt-token", response.getToken());
        verify(authService, times(1)).login(request);
    }
}
