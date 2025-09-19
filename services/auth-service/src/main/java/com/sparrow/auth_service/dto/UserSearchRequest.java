package com.sparrow.auth_service.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserSearchRequest {
    private String searchTerm;
    private List<String> roles;
    private Boolean enabled;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdTimestamp";
    private String sortDirection = "DESC";
}