package com.networkmonitor.model;

import java.sql.Timestamp;

/**
 * SecurityEvent - POJO representing a detected security threat/incident
 * Maps to: security_events table
 */
public class SecurityEvent {
    private int eventId;
    private int deviceId;
    private String eventType; // DDOS, BRUTE_FORCE, PORT_SCAN, MALWARE, UNAUTHORIZED_ACCESS
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String sourceIp;
    private String description;
    private boolean isResolved;
    private Timestamp detectedAt;
    private Timestamp resolvedAt;

    // Constructors
    public SecurityEvent() {
    }

    public SecurityEvent(int deviceId, String eventType, String severity, String sourceIp, String description) {
        this.deviceId = deviceId;
        this.eventType = eventType;
        this.severity = severity;
        this.sourceIp = sourceIp;
        this.description = description;
        this.isResolved = false;
    }

    public SecurityEvent(int eventId, int deviceId, String eventType, String severity,
                         String sourceIp, String description, boolean isResolved,
                         Timestamp detectedAt, Timestamp resolvedAt) {
        this.eventId = eventId;
        this.deviceId = deviceId;
        this.eventType = eventType;
        this.severity = severity;
        this.sourceIp = sourceIp;
        this.description = description;
        this.isResolved = isResolved;
        this.detectedAt = detectedAt;
        this.resolvedAt = resolvedAt;
    }

    // Getters and Setters
    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setResolved(boolean resolved) {
        isResolved = resolved;
    }

    public Timestamp getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Timestamp detectedAt) {
        this.detectedAt = detectedAt;
    }

    public Timestamp getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Timestamp resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    @Override
    public String toString() {
        return "SecurityEvent{" +
                "eventId=" + eventId +
                ", deviceId=" + deviceId +
                ", eventType='" + eventType + '\'' +
                ", severity='" + severity + '\'' +
                ", sourceIp='" + sourceIp + '\'' +
                ", isResolved=" + isResolved +
                ", detectedAt=" + detectedAt +
                ", resolvedAt=" + resolvedAt +
                '}';
    }
}
