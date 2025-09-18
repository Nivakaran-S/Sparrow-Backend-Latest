package com.sparrow.auth_service.dto;


import lombok.Data;
import java.util.List;
import java.time.LocalDateTime;

@Data
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private boolean enabled;
    private LocalDateTime createdTimestamp;
    private String phoneNumber;
    private String address;
}