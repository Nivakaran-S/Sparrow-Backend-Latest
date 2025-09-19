package com.sparrow.auth_service.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;
import java.util.Map;

@Data
public class UserUpdateRequest {
    private String firstName;
    private String lastName;

    @Email(message = "Email should be valid")
    private String email;

    private String phoneNumber;
    private String address;
    private Map<String, String> additionalAttributes;
}