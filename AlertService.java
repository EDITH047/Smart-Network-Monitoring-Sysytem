package com.networkmonitor.service;

import com.networkmonitor.dao.AlertDAO;
import com.networkmonitor.model.Alert;
import com.networkmonitor.model.NetworkMetric;
import java.util.List;

/**
 * AlertService - Handles threshold-based alerts and notifications
 * Uses: AlertDAO
 */
public class AlertService {

    private static final AlertService instance = new AlertService();
    private AlertDAO alertDAO = new AlertDAO();

    // Configurable thresholds
    private double bandwidthThreshold = 90.0; // 90% utilization
    private double latencyThreshold = 200.0; // 200ms
    private double packetLossThreshold = 5.0; // 5%

    private AlertService() {
    }

    public static AlertService getInstance() {
        return instance;
    }

    // Debounce maps for smart alerting
    private final java.util.concurrent.ConcurrentHashMap<String, Integer> consecutiveViolations = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Long> lastAlertTime = new java.util.concurrent.ConcurrentHashMap<>();
    
    private static final int REQUIRED_VIOLATIONS = 3;
    private static final long COOLDOWN_MS = 15 * 60 * 1000; // 15 minutes cooldown

    /**
     * Check thresholds for a metric and create alerts using hysteresis and cooldowns.
     */
    public void checkThresholds(NetworkMetric metric) {
        if (metric == null) return;
        
        int deviceId = metric.getDeviceId();
        boolean isOffline = metric.getLatencyMs() >= 9999 || metric.getPacketLossPct() >= 100.0;
        
        // 1. Device Offline State
        checkState(deviceId, "DEVICE_OFFLINE", isOffline, "CRITICAL", "Device is OFFLINE or completely unreachable.");
        
        // 2. Latency State (Only check if device is online)
        boolean highLatency = !isOffline && metric.getLatencyMs() > latencyThreshold;
        checkState(deviceId, "LATENCY_THRESHOLD", highLatency, "WARNING", 
                   "High latency detected: " + String.format("%.1f", metric.getLatencyMs()) + "ms");
                   
        // 3. Packet Loss State (Only check if device is online)
        boolean highPacketLoss = !isOffline && metric.getPacketLossPct() > packetLossThreshold;
        checkState(deviceId, "PACKET_LOSS_THRESHOLD", highPacketLoss, "WARNING", 
                   "Packet loss detected: " + String.format("%.1f", metric.getPacketLossPct()) + "%");
                   
        // 4. Bandwidth State
        boolean highBandwidth = metric.getBandwidthUsage() > bandwidthThreshold;
        checkState(deviceId, "BANDWIDTH_THRESHOLD", highBandwidth, "CRITICAL", 
                   "High bandwidth usage: " + String.format("%.1f", metric.getBandwidthUsage()) + "%");
    }

    private void checkState(int deviceId, String alertType, boolean isViolating, String severity, String message) {
        String key = deviceId + "_" + alertType;
        
        if (isViolating) {
            int violations = consecutiveViolations.getOrDefault(key, 0) + 1;
            consecutiveViolations.put(key, violations);
            
            if (violations >= REQUIRED_VIOLATIONS) {
                long now = System.currentTimeMillis();
                long lastTime = lastAlertTime.getOrDefault(key, 0L);
                
                // Only alert if we've passed the cooldown
                if (now - lastTime > COOLDOWN_MS) {
                    createAlert(deviceId, alertType, severity, message);
                    lastAlertTime.put(key, now); // Reset the cooldown timer
                }
            }
        } else {
            // Immediately reset violation counter if metric returns to normal
            consecutiveViolations.remove(key);
        }
    }

    /**
     * Create a new alert
     */
    public boolean createAlert(int deviceId, String alertType, String severity, String message) {
        Alert alert = new Alert(deviceId, alertType, severity, message);
        boolean success = alertDAO.insertAlert(alert);

        if (success) {
            System.out.println("[AlertService] Alert created: " + severity + " - " + alertType);
            if ("CRITICAL".equals(severity) || "HIGH".equals(severity)) {
                try {
                    com.networkmonitor.service.NotificationService.getInstance().showNotification(
                        "Network Alert (" + severity + ")", message, 
                        java.awt.TrayIcon.MessageType.WARNING
                    );
                } catch (Exception e) {}
            }
        }

        return success;
    }

    /**
     * Get unacknowledged alerts count (for dashboard badge)
     */
    public int getUnacknowledgedCount() {
        return alertDAO.getUnacknowledgedCount();
    }

    /**
     * Get all unacknowledged alerts
     */
    public List<Alert> getUnacknowledgedAlerts() {
        return alertDAO.getUnacknowledgedAlerts();
    }

    /**
     * Get critical alerts
     */
    public List<Alert> getCriticalAlerts() {
        return alertDAO.getCriticalAlerts();
    }

    /**
     * Get alerts for a device
     */
    public List<Alert> getAlertsByDevice(int deviceId) {
        return alertDAO.getAlertsByDevice(deviceId);
    }

    /**
     * Acknowledge an alert
     */
    public boolean acknowledgeAlert(int alertId, int userId) {
        return alertDAO.acknowledgeAlert(alertId, userId);
    }

    /**
     * Get recent alerts
     */
    public List<Alert> getRecentAlerts(int limit) {
        return alertDAO.getRecentAlerts(limit);
    }

    /**
     * Set bandwidth threshold
     */
    public void setBandwidthThreshold(double threshold) {
        this.bandwidthThreshold = threshold;
        System.out.println("[AlertService] Bandwidth threshold set to: " + threshold + "%");
    }

    /**
     * Set latency threshold
     */
    public void setLatencyThreshold(double threshold) {
        this.latencyThreshold = threshold;
        System.out.println("[AlertService] Latency threshold set to: " + threshold + "ms");
    }

    /**
     * Set packet loss threshold
     */
    public void setPacketLossThreshold(double threshold) {
        this.packetLossThreshold = threshold;
        System.out.println("[AlertService] Packet loss threshold set to: " + threshold + "%");
    }
}
