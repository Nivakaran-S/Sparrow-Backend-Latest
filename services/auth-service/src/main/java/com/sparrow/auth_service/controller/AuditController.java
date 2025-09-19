package com.sparrow.auth_service.controller;

import com.sparrow.auth_service.dto.AuditLogResponse;
import com.sparrow.auth_service.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "User activity audit log APIs")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private static final Logger logger = LoggerFactory.getLogger(AuditController.class);

    private final AuditService auditService;

    @GetMapping("/logs")
    @Operation(summary = "Get audit logs", description = "Get paginated audit logs with optional filters (Admin only)")
    public ResponseEntity<?> getAuditLogs(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by user ID") @RequestParam(required = false) String userId,
            @Parameter(description = "Filter by action") @RequestParam(required = false) String action,
            @Parameter(description = "Start date") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<AuditLogResponse> auditLogs = auditService.getAuditLogs(pageable, userId, action, startDate, endDate);
            return ResponseEntity.ok(auditLogs);
        } catch (Exception e) {
            logger.error("Failed to get audit logs", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to retrieve audit logs", "message", e.getMessage()));
        }
    }

    @GetMapping("/logs/user/{userId}")
    @Operation(summary = "Get user audit logs", description = "Get audit logs for specific user (Admin only)")
    public ResponseEntity<?> getUserAuditLogs(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<AuditLogResponse> auditLogs = auditService.getUserAuditLogs(userId, pageable);
            return ResponseEntity.ok(auditLogs);
        } catch (Exception e) {
            logger.error("Failed to get user audit logs for user: {}", userId, e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to retrieve user audit logs", "message", e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get audit statistics", description = "Get audit activity statistics (Admin only)")
    public ResponseEntity<?> getAuditStatistics(
            @Parameter(description = "Number of days to look back") @RequestParam(defaultValue = "30") int days) {

        try {
            Map<String, Object> statistics = auditService.getAuditStatistics(days);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Failed to get audit statistics", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to retrieve audit statistics", "message", e.getMessage()));
        }
    }
}

// AuditLogResponse.java

// AuditService.java
