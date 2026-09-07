package com.networkmonitor.service;

import com.networkmonitor.dao.*;
import com.networkmonitor.model.*;
import java.sql.Timestamp;
import java.util.*;

/**
 * ReportService - Handles report generation and data export
 * Uses: All DAOs for data aggregation
 */
public class ReportService {

    private static ReportService instance;
    private DeviceDAO deviceDAO = new DeviceDAO();
    private NetworkMetricDAO metricDAO = new NetworkMetricDAO();
    private SecurityEventDAO eventDAO = new SecurityEventDAO();
    private AlertDAO alertDAO = new AlertDAO();
    private AuditLogDAO auditLogDAO = new AuditLogDAO();

    private ReportService() {
    }

    public static ReportService getInstance() {
        if (instance == null) {
            instance = new ReportService();
        }
        return instance;
    }

    /**
     * Generate network health report
     */
    public String[][] generateNetworkHealthReport(Timestamp from, Timestamp to) {
        System.out.println("[ReportService] Generating network health report...");

        List<Device> devices = deviceDAO.getAllDevices();
        List<String[]> reportData = new ArrayList<>();

        // Add headers
        reportData.add(new String[]{"Device Name", "Type", "Status", "Avg Bandwidth", "Avg Latency", "Packet Loss"});

        for (Device device : devices) {
            NetworkMetric avgMetric = metricDAO.getAverageMetrics(device.getDeviceId(), 24);

            if (avgMetric != null) {
                reportData.add(new String[]{
                        device.getDeviceName(),
                        device.getDeviceType(),
                        device.getStatus(),
                        String.format("%.2f Mbps", avgMetric.getBandwidthUsage()),
                        String.format("%.2f ms", avgMetric.getLatencyMs()),
                        String.format("%.2f%%", avgMetric.getPacketLossPct())
                });
            }
        }

        return reportData.toArray(new String[0][]);
    }

    /**
     * Generate security incident report
     */
    public String[][] generateSecurityReport(Timestamp from, Timestamp to) {
        System.out.println("[ReportService] Generating security report...");

        List<SecurityEvent> events = eventDAO.getEventsByDateRange(from, to);
        List<String[]> reportData = new ArrayList<>();

        // Add headers
        reportData.add(new String[]{"Date", "Type", "Severity", "Device", "Source IP", "Status"});

        for (SecurityEvent event : events) {
            Device device = deviceDAO.getDeviceById(event.getDeviceId());
            String deviceName = device != null ? device.getDeviceName() : "Unknown";

            reportData.add(new String[]{
                    event.getDetectedAt().toString(),
                    event.getEventType(),
                    event.getSeverity(),
                    deviceName,
                    event.getSourceIp() != null ? event.getSourceIp() : "N/A",
                    event.isResolved() ? "Resolved" : "Open"
            });
        }

        return reportData.toArray(new String[0][]);
    }

    /**
     * Generate bandwidth utilization report
     */
    public String[][] generateBandwidthReport(Timestamp from, Timestamp to) {
        System.out.println("[ReportService] Generating bandwidth report...");

        List<Device> devices = deviceDAO.getAllDevices();
        List<String[]> reportData = new ArrayList<>();

        // Add headers
        reportData.add(new String[]{"Device Name", "Avg Usage", "Peak Usage", "Min Usage", "Utilization %"});

        for (Device device : devices) {
            NetworkMetric avgMetric = metricDAO.getAverageMetrics(device.getDeviceId(), 24);
            double peakBandwidth = metricDAO.getPeakBandwidth(device.getDeviceId(), 24);

            if (avgMetric != null) {
                double utilizationPct = (avgMetric.getBandwidthUsage() / 100.0) * 100;

                reportData.add(new String[]{
                        device.getDeviceName(),
                        String.format("%.2f Mbps", avgMetric.getBandwidthUsage()),
                        String.format("%.2f Mbps", peakBandwidth),
                        String.format("%.2f Mbps", Math.min(avgMetric.getBandwidthUsage(), peakBandwidth / 2)),
                        String.format("%.1f%%", utilizationPct)
                });
            }
        }

        return reportData.toArray(new String[0][]);
    }

    /**
     * Generate alert summary report
     */
    public String[][] generateAlertSummaryReport(Timestamp from, Timestamp to) {
        System.out.println("[ReportService] Generating alert summary report...");

        List<Alert> alerts = new ArrayList<>();
        List<String[]> reportData = new ArrayList<>();

        // Add headers
        reportData.add(new String[]{"Date", "Severity", "Type", "Device", "Message", "Status"});

        // Get recent alerts
        List<Alert> recentAlerts = alertDAO.getRecentAlerts(1000);
        for (Alert alert : recentAlerts) {
            if (alert.getCreatedAt().before(to) && alert.getCreatedAt().after(from)) {
                Device device = deviceDAO.getDeviceById(alert.getDeviceId());
                String deviceName = device != null ? device.getDeviceName() : "Unknown";

                reportData.add(new String[]{
                        alert.getCreatedAt().toString(),
                        alert.getSeverity(),
                        alert.getAlertType(),
                        deviceName,
                        alert.getMessage(),
                        alert.isAcknowledged() ? "Acknowledged" : "Open"
                });
            }
        }

        return reportData.toArray(new String[0][]);
    }

    /**
     * Generate audit log report
     */
    public String[][] generateAuditReport(Timestamp from, Timestamp to) {
        System.out.println("[ReportService] Generating audit report...");

        List<AuditLog> logs = auditLogDAO.getLogsByDateRange(from, to);
        List<String[]> reportData = new ArrayList<>();

        // Add headers
        reportData.add(new String[]{"Date", "User", "Action", "Details", "IP Address"});

        for (AuditLog log : logs) {
            User user = new com.networkmonitor.dao.UserDAO().findById(log.getUserId());
            String username = user != null ? user.getUsername() : "Unknown";

            reportData.add(new String[]{
                    log.getPerformedAt().toString(),
                    username,
                    log.getAction(),
                    log.getDetails() != null ? log.getDetails() : "",
                    log.getIpAddress() != null ? log.getIpAddress() : "N/A"
            });
        }

        return reportData.toArray(new String[0][]);
    }

    /**
     * Generate device performance summary
     */
    public String[][] generateDevicePerformanceSummary() {
        System.out.println("[ReportService] Generating device performance summary...");

        List<Device> devices = deviceDAO.getAllDevices();
        List<String[]> reportData = new ArrayList<>();

        // Add summary info
        reportData.add(new String[]{"Metric", "Value"});
        reportData.add(new String[]{"Total Devices", String.valueOf(deviceDAO.getDeviceCount())});
        reportData.add(new String[]{"Online Devices", String.valueOf(deviceDAO.getOnlineDeviceCount())});
        reportData.add(new String[]{"System Health", String.format("%.1f%%", MonitoringService.getInstance().getSystemHealth())});
        reportData.add(new String[]{"Average Bandwidth", String.format("%.2f Mbps", MonitoringService.getInstance().getAverageBandwidth())});
        reportData.add(new String[]{"Network Optimization Score", String.format("%.1f", OptimizationService.getInstance().getNetworkAverageScore())});
        reportData.add(new String[]{"Unacknowledged Alerts", String.valueOf(alertDAO.getUnacknowledgedCount())});
        reportData.add(new String[]{"Unresolved Security Events", String.valueOf(new SecurityEventDAO().getUnresolvedEvents().size())});

        return reportData.toArray(new String[0][]);
    }

    /**
     * Get report as formatted string for display
     */
    public String formatReportAsString(String reportType, Timestamp from, Timestamp to) {
        StringBuilder sb = new StringBuilder();
        String[][] data;

        sb.append("================================================================================\n");
        sb.append(reportType).append("\n");
        sb.append("Generated: ").append(new java.util.Date()).append("\n");
        sb.append("Period: ").append(from).append(" to ").append(to).append("\n");
        sb.append("================================================================================\n\n");

        if ("NETWORK_HEALTH".equals(reportType)) {
            data = generateNetworkHealthReport(from, to);
        } else if ("SECURITY".equals(reportType)) {
            data = generateSecurityReport(from, to);
        } else if ("BANDWIDTH".equals(reportType)) {
            data = generateBandwidthReport(from, to);
        } else if ("ALERTS".equals(reportType)) {
            data = generateAlertSummaryReport(from, to);
        } else if ("AUDIT".equals(reportType)) {
            data = generateAuditReport(from, to);
        } else {
            data = generateDevicePerformanceSummary();
        }

        // Format as table
        for (String[] row : data) {
            for (String cell : row) {
                sb.append(String.format("%-25s", cell));
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
