package com.networkmonitor.service;

import com.networkmonitor.dao.BlockedIPDAO;
import com.networkmonitor.dao.SecurityEventDAO;
import com.networkmonitor.model.NetworkMetric;
import com.networkmonitor.model.SecurityEvent;
import java.util.List;

/**
 * SecurityService - Handles threat detection and security event management
 * Uses: SecurityEventDAO, BlockedIPDAO
 */
public class SecurityService {

    private static SecurityService instance;
    private SecurityEventDAO eventDAO = new SecurityEventDAO();
    private BlockedIPDAO blockedIPDAO = new BlockedIPDAO();

    // Thresholds for threat detection
    private static final long PACKETS_PER_SEC_THRESHOLD = 100000; // DDoS threshold
    private static final double LATENCY_SPIKE_THRESHOLD = 200; // ms
    private static final double PACKET_LOSS_THRESHOLD = 5; // percent

    private SecurityService() {
    }

    public static SecurityService getInstance() {
        if (instance == null) {
            instance = new SecurityService();
        }
        return instance;
    }

    /**
     * Analyze metric for security threats
     */
    public SecurityEvent analyze(NetworkMetric metric) {
        SecurityEvent event = null;

        // Check for DDoS (high packet rate)
        if (metric.getPacketsIn() > PACKETS_PER_SEC_THRESHOLD) {
            event = new SecurityEvent(metric.getDeviceId(), "DDOS", "CRITICAL",
                    "Unknown", "Detected unusually high packet rate: " + metric.getPacketsIn() + " packets/sec");
            System.out.println("[SecurityService] DDOS detected on device: " + metric.getDeviceId());
        }

        // Check for brute force (high latency + packet loss)
        else if (metric.getLatencyMs() > LATENCY_SPIKE_THRESHOLD && metric.getPacketLossPct() > 3) {
            event = new SecurityEvent(metric.getDeviceId(), "BRUTE_FORCE", "HIGH",
                    "Unknown", "Suspected brute force attack - high latency and packet loss");
            System.out.println("[SecurityService] Brute force suspected on device: " + metric.getDeviceId());
        }

        // Check for port scan (packet loss spike)
        else if (metric.getPacketLossPct() > PACKET_LOSS_THRESHOLD) {
            event = new SecurityEvent(metric.getDeviceId(), "PORT_SCAN", "MEDIUM",
                    "Unknown", "Possible port scan detected - high packet loss");
            System.out.println("[SecurityService] Port scan suspected on device: " + metric.getDeviceId());
        }

        // Log event and auto-block if critical
        if (event != null) {
            eventDAO.insertEvent(event);

            // Auto-block for critical threats
            if ("CRITICAL".equals(event.getSeverity())) {
                blockedIPDAO.blockIPWithExpiry("0.0.0.0", "Auto-blocked: " + event.getEventType(), 1, 24);

                // Trigger alert
                AlertService alertService = AlertService.getInstance();
                alertService.createAlert(metric.getDeviceId(), "SECURITY_THREAT",
                        "CRITICAL", "Critical threat detected: " + event.getEventType());
            }

            return event;
        }

        return null;
    }

    /**
     * Get all unresolved security events
     */
    public List<SecurityEvent> getUnresolvedEvents() {
        return eventDAO.getUnresolvedEvents();
    }

    /**
     * Get events by device
     */
    public List<SecurityEvent> getEventsByDevice(int deviceId) {
        return eventDAO.getEventsByDevice(deviceId);
    }

    /**
     * Get critical events
     */
    public List<SecurityEvent> getCriticalEvents() {
        return eventDAO.getCriticalEvents();
    }

    /**
     * Resolve a security event
     */
    public boolean resolveEvent(int eventId) {
        return eventDAO.resolveEvent(eventId);
    }

    /**
     * Check if IP is blocked
     */
    public boolean isIPBlocked(String ipAddress) {
        return blockedIPDAO.isIPBlocked(ipAddress);
    }

    /**
     * Block an IP address
     */
    public boolean blockIP(String ipAddress, String reason, int userId) {
        return blockedIPDAO.blockIP(ipAddress, reason, userId, false);
    }

    /**
     * Unblock an IP address
     */
    public boolean unblockIP(String ipAddress) {
        return blockedIPDAO.unblockIPByAddress(ipAddress);
    }

    /**
     * Get threat count by type
     */
    public int getThreatCount(String threatType) {
        return eventDAO.getEventCountByType(threatType);
    }
}
