package com.networkmonitor.model;

import java.sql.Timestamp;

/**
 * AuditLog - POJO representing an audit log entry (user actions)
 * Maps to: audit_log table
 */
public class AuditLog {
    private int logId;
    private int userId;
    private String action;
    private String details;
    private String ipAddress;
    private Timestamp performedAt;

    // Constructors
    public AuditLog() {
    }

    public AuditLog(int userId, String action, String details, String ipAddress) {
        this.userId = userId;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
    }

    public AuditLog(int logId, int userId, String action, String details, String ipAddress, Timestamp performedAt) {
        this.logId = logId;
        this.userId = userId;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        this.performedAt = performedAt;
    }

    // Getters and Setters
    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Timestamp getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(Timestamp performedAt) {
        this.performedAt = performedAt;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "logId=" + logId +
                ", userId=" + userId +
                ", action='" + action + '\'' +
                ", details='" + details + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", performedAt=" + performedAt +
                '}';
    }
}
