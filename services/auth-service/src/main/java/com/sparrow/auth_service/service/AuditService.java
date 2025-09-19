package com.sparrow.auth_service.service;

import com.sparrow.auth_service.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    // In-memory storage for demo purposes
    // In production, use a proper database like MongoDB or PostgreSQL
    private final List<AuditLogResponse> auditLogs = new ArrayList<>();

    public void logAction(String userId, String username, String action, String details,
                          String ipAddress, String userAgent, String performedBy, boolean success) {
        AuditLogResponse auditLog = new AuditLogResponse();
        auditLog.setId(UUID.randomUUID().toString());
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setDetails(details);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setPerformedBy(performedBy);
        auditLog.setSuccess(success);

        auditLogs.add(auditLog);

        // Keep only last 10000 logs in memory
        if (auditLogs.size() > 10000) {
            auditLogs.remove(0);
        }

        log.info("Audit log created: {} - {} by {}", action, success ? "SUCCESS" : "FAILURE", performedBy);
    }

    public Page<AuditLogResponse> getAuditLogs(Pageable pageable, String userId, String action,
                                               LocalDateTime startDate, LocalDateTime endDate) {
        List<AuditLogResponse> filteredLogs = auditLogs.stream()
                .filter(log -> userId == null || userId.equals(log.getUserId()))
                .filter(log -> action == null || action.equalsIgnoreCase(log.getAction()))
                .filter(log -> startDate == null || log.getTimestamp().isAfter(startDate))
                .filter(log -> endDate == null || log.getTimestamp().isBefore(endDate))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredLogs.size());

        List<AuditLogResponse> pageContent = start < filteredLogs.size()
                ? filteredLogs.subList(start, end)
                : new ArrayList<>();

        return new PageImpl<>(pageContent, pageable, filteredLogs.size());
    }

    public Page<AuditLogResponse> getUserAuditLogs(String userId, Pageable pageable) {
        return getAuditLogs(pageable, userId, null, null, null);
    }

    public Map<String, Object> getAuditStatistics(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

        List<AuditLogResponse> recentLogs = auditLogs.stream()
                .filter(log -> log.getTimestamp().isAfter(cutoff))
                .toList();

        Map<String, Object> statistics = new HashMap<>();

        // Total activities
        statistics.put("totalActivities", recentLogs.size());

        // Success/Failure counts
        long successCount = recentLogs.stream().mapToLong(log -> log.isSuccess() ? 1 : 0).sum();
        statistics.put("successfulActivities", successCount);
        statistics.put("failedActivities", recentLogs.size() - successCount);

        // Activity breakdown by action
        Map<String, Long> actionCounts = recentLogs.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        AuditLogResponse::getAction,
                        java.util.stream.Collectors.counting()
                ));
        statistics.put("activitiesByAction", actionCounts);

        // Daily activity counts
        Map<String, Long> dailyCounts = recentLogs.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        log -> log.getTimestamp().toLocalDate().toString(),
                        java.util.stream.Collectors.counting()
                ));
        statistics.put("dailyActivityCounts", dailyCounts);

        // Most active users
        Map<String, Long> userCounts = recentLogs.stream()
                .filter(log -> log.getUsername() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        AuditLogResponse::getUsername,
                        java.util.stream.Collectors.counting()
                ));

        List<Map<String, Object>> topUsers = userCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> userStat = new HashMap<>();
                    userStat.put("username", entry.getKey());
                    userStat.put("activityCount", entry.getValue());
                    return userStat;
                })
                .toList();
        statistics.put("mostActiveUsers", topUsers);

        return statistics;
    }

    public void logUserLogin(String userId, String username, String ipAddress, String userAgent, boolean success) {
        logAction(userId, username, "LOGIN",
                success ? "User logged in successfully" : "Failed login attempt",
                ipAddress, userAgent, username, success);
    }

    public void logUserLogout(String userId, String username, String ipAddress, String userAgent) {
        logAction(userId, username, "LOGOUT", "User logged out",
                ipAddress, userAgent, username, true);
    }

    public void logUserRegistration(String userId, String username, String ipAddress, String userAgent, boolean success) {
        logAction(userId, username, "REGISTRATION",
                success ? "User registered successfully" : "Failed registration attempt",
                ipAddress, userAgent, "SYSTEM", success);
    }

    public void logPasswordChange(String userId, String username, String performedBy, boolean success) {
        logAction(userId, username, "PASSWORD_CHANGE",
                success ? "Password changed successfully" : "Failed password change attempt",
                null, null, performedBy, success);
    }

    public void logRoleChange(String userId, String username, String newRoles, String performedBy) {
        logAction(userId, username, "ROLE_CHANGE",
                "Roles updated to: " + newRoles,
                null, null, performedBy, true);
    }

    public void logUserStatusChange(String userId, String username, boolean enabled, String performedBy) {
        logAction(userId, username, enabled ? "USER_ENABLED" : "USER_DISABLED",
                "User account " + (enabled ? "enabled" : "disabled"),
                null, null, performedBy, true);
    }

    public void logUserDeletion(String userId, String username, String performedBy) {
        logAction(userId, username, "USER_DELETED",
                "User account permanently deleted",
                null, null, performedBy, true);
    }
}