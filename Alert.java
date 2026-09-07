package com.networkmonitor.model;

import java.sql.Timestamp;

/**
 * Alert - POJO representing a threshold-based alert
 * Maps to: alerts table
 */
public class Alert {
    private int alertId;
    private int deviceId;
    private String alertType;
    private String severity; // INFO, WARNING, CRITICAL
    private String message;
    private boolean isAcknowledged;
    private Timestamp createdAt;
    private int acknowledgedBy; // user_id

    // Constructors
    public Alert() {
    }

    public Alert(int deviceId, String alertType, String severity, String message) {
        this.deviceId = deviceId;
        this.alertType = alertType;
        this.severity = severity;
        this.message = message;
        this.isAcknowledged = false;
    }

    public Alert(int alertId, int deviceId, String alertType, String severity, String message,
                 boolean isAcknowledged, Timestamp createdAt, int acknowledgedBy) {
        this.alertId = alertId;
        this.deviceId = deviceId;
        this.alertType = alertType;
        this.severity = severity;
        this.message = message;
        this.isAcknowledged = isAcknowledged;
        this.createdAt = createdAt;
        this.acknowledgedBy = acknowledgedBy;
    }

    // Getters and Setters
    public int getAlertId() {
        return alertId;
    }

    public void setAlertId(int alertId) {
        this.alertId = alertId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isAcknowledged() {
        return isAcknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        isAcknowledged = acknowledged;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(int acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    @Override
    public String toString() {
        return "Alert{" +
                "alertId=" + alertId +
                ", deviceId=" + deviceId +
                ", alertType='" + alertType + '\'' +
                ", severity='" + severity + '\'' +
                ", message='" + message + '\'' +
                ", isAcknowledged=" + isAcknowledged +
                ", createdAt=" + createdAt +
                '}';
    }
}
