package com.sparrow.auth_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogResponse {
    private String id;
    private String userId;
    private String username;
    private String action;
    private String details;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
    private String performedBy;
    private boolean success;
}
