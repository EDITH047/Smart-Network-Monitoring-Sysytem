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

    private static AlertService instance;
    private AlertDAO alertDAO = new AlertDAO();

    // Configurable thresholds
    private double bandwidthThreshold = 90.0; // 90% utilization
    private double latencyThreshold = 200.0; // 200ms
    private double packetLossThreshold = 5.0; // 5%

    private AlertService() {
    }

    public static AlertService getInstance() {
        if (instance == null) {
            instance = new AlertService();
        }
        return instance;
    }

    /**
     * Check thresholds for a metric and create alerts if needed
     */
    public void checkThresholds(NetworkMetric metric) {
        if (metric == null) {
            return;
        }

        // Check bandwidth
        if (metric.getBandwidthUsage() > bandwidthThreshold) {
            createAlert(metric.getDeviceId(), "BANDWIDTH_THRESHOLD",
                    "CRITICAL", "High bandwidth usage: " + String.format("%.1f", metric.getBandwidthUsage()) + "%");
        }

        // Check latency
        if (metric.getLatencyMs() > latencyThreshold) {
            createAlert(metric.getDeviceId(), "LATENCY_THRESHOLD",
                    "WARNING", "High latency detected: " + String.format("%.1f", metric.getLatencyMs()) + "ms");
        }

        // Check packet loss
        if (metric.getPacketLossPct() > packetLossThreshold) {
            createAlert(metric.getDeviceId(), "PACKET_LOSS_THRESHOLD",
                    "WARNING", "Packet loss detected: " + String.format("%.1f", metric.getPacketLossPct()) + "%");
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
