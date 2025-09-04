package com.bharath.media_backend.api.dto;

import lombok.*;

@Getter @Setter
public class LoginRequest {
    private String email;
    private String password;
}