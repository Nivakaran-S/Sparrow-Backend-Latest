package com.sparrow.auth_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private String username;
    private String email;
    private List<String> roles;
    private String userId;
}