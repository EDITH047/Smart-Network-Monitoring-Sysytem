package com.networkmonitor.service;

import com.networkmonitor.dao.DeviceDAO;
import com.networkmonitor.dao.NetworkMetricDAO;
import com.networkmonitor.model.Device;
import com.networkmonitor.model.NetworkMetric;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MonitoringService - Handles network device monitoring and metric collection
 * Uses: DeviceDAO, NetworkMetricDAO
 */
public class MonitoringService {

    private static MonitoringService instance;
    private DeviceDAO deviceDAO = new DeviceDAO();
    private NetworkMetricDAO metricDAO = new NetworkMetricDAO();
    private Random random = new Random();

    private MonitoringService() {
    }

    public static MonitoringService getInstance() {
        if (instance == null) {
            instance = new MonitoringService();
        }
        return instance;
    }

    /**
     * Collect metrics from all devices
     * This runs periodically (every 10 seconds in UI)
     */
    public List<NetworkMetric> collectMetrics() {
        List<NetworkMetric> metrics = new ArrayList<>();
        List<Device> devices = deviceDAO.getAllDevices();

        for (Device device : devices) {
            try {
                // Check if device is reachable
                boolean isReachable = isDeviceReachable(device.getIpAddress());

                // Generate metric data
                NetworkMetric metric = generateMetric(device.getDeviceId(), isReachable);

                // Save to database
                if (metricDAO.insertMetric(metric)) {
                    metrics.add(metric);

                    // Update device status
                    String status = isReachable ? "ONLINE" : "OFFLINE";
                    deviceDAO.updateDeviceStatus(device.getDeviceId(), status);
                }
            } catch (Exception e) {
                System.err.println("[MonitoringService] Error collecting metric for device " + device.getDeviceId() + ": " + e.getMessage());
            }
        }

        System.out.println("[MonitoringService] Collected metrics from " + metrics.size() + " devices");
        return metrics;
    }

    /**
     * Get latest metrics for all devices
     */
    public List<NetworkMetric> getLatestMetrics() {
        return metricDAO.getLatestMetrics();
    }

    /**
     * Get latest metric for a specific device
     */
    public NetworkMetric getLatestMetricByDevice(int deviceId) {
        return metricDAO.getLatestByDevice(deviceId);
    }

    /**
     * Check if device is reachable (ping)
     */
    private boolean isDeviceReachable(String ipAddress) {
        try {
            InetAddress inet = InetAddress.getByName(ipAddress);
            return inet.isReachable(3000); // 3 second timeout
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate realistic metric data (simulated for demo)
     */
    private NetworkMetric generateMetric(int deviceId, boolean isOnline) {
        NetworkMetric metric = new NetworkMetric();
        metric.setDeviceId(deviceId);

        if (isOnline) {
            // Generate realistic data for online device
            metric.setBandwidthUsage(20 + random.nextDouble() * 70); // 20-90 Mbps
            metric.setLatencyMs(10 + random.nextDouble() * 80); // 10-90 ms
            metric.setPacketLossPct(random.nextDouble() * 2); // 0-2%
            metric.setPacketsIn(100000 + random.nextLong() % 500000);
            metric.setPacketsOut(50000 + random.nextLong() % 300000);
        } else {
            // Offline device has no traffic
            metric.setBandwidthUsage(0);
            metric.setLatencyMs(0);
            metric.setPacketLossPct(100);
            metric.setPacketsIn(0);
            metric.setPacketsOut(0);
        }

        return metric;
    }

    /**
     * Get average metrics for a device over N hours
     */
    public NetworkMetric getAverageMetrics(int deviceId, int hours) {
        return metricDAO.getAverageMetrics(deviceId, hours);
    }

    /**
     * Get peak bandwidth for a device
     */
    public double getPeakBandwidth(int deviceId, int hours) {
        return metricDAO.getPeakBandwidth(deviceId, hours);
    }

    /**
     * Get overall system health (percentage of online devices)
     */
    public double getSystemHealth() {
        int totalDevices = deviceDAO.getDeviceCount();
        if (totalDevices == 0) {
            return 0;
        }

        int onlineDevices = deviceDAO.getOnlineDeviceCount();
        return (onlineDevices * 100.0) / totalDevices;
    }

    /**
     * Get average bandwidth across all devices
     */
    public double getAverageBandwidth() {
        List<NetworkMetric> metrics = getLatestMetrics();
        if (metrics.isEmpty()) {
            return 0;
        }

        double total = 0;
        for (NetworkMetric metric : metrics) {
            total += metric.getBandwidthUsage();
        }

        return total / metrics.size();
    }

    /**
     * Get devices with high bandwidth usage
     */
    public List<Device> getHighBandwidthDevices(double threshold) {
        List<Device> highBandwidthDevices = new ArrayList<>();
        List<NetworkMetric> metrics = getLatestMetrics();

        for (NetworkMetric metric : metrics) {
            if (metric.getBandwidthUsage() > threshold) {
                Device device = deviceDAO.getDeviceById(metric.getDeviceId());
                if (device != null) {
                    highBandwidthDevices.add(device);
                }
            }
        }

        return highBandwidthDevices;
    }

    /**
     * Get devices with high latency
     */
    public List<Device> getHighLatencyDevices(double threshold) {
        List<Device> highLatencyDevices = new ArrayList<>();
        List<NetworkMetric> metrics = getLatestMetrics();

        for (NetworkMetric metric : metrics) {
            if (metric.getLatencyMs() > threshold) {
                Device device = deviceDAO.getDeviceById(metric.getDeviceId());
                if (device != null) {
                    highLatencyDevices.add(device);
                }
            }
        }

        return highLatencyDevices;
    }
}
