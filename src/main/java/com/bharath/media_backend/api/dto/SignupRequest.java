package com.bharath.media_backend.api.dto;

import lombok.*;

@Getter @Setter
public class SignupRequest {
    private String email;
    private String password;
}
