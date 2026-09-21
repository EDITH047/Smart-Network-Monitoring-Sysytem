package com.networkmonitor.service;

import com.networkmonitor.dao.DeviceDAO;
import com.networkmonitor.dao.NetworkMetricDAO;
import com.networkmonitor.model.Device;
import com.networkmonitor.model.NetworkMetric;
import com.networkmonitor.util.NetworkAdapter;
import com.networkmonitor.util.NetworkAdapter.SystemNetworkStats;
import com.networkmonitor.util.NetworkAdapter.InterfaceStats;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * MonitoringService - Enhanced version with real network data collection
 *
 * This service collects REAL network metrics from the system using NetworkAdapter
 * and maps them to monitored devices. It can operate in two modes:
 *
 * 1. REAL MODE: Collects actual network statistics from system interfaces
 * 2. SIMULATED MODE: Generates random metrics for demo purposes
 *
 * Integration with your project architecture:
 * - Called by MonitoringPanel's Timer (every 10 seconds)
 * - Uses DeviceDAO to get monitored devices
 * - Uses NetworkMetricDAO to persist metrics
 * - Triggers AlertService for threshold checking
 * - Triggers SecurityService for threat detection
 *
 * @author Network Monitor Team
 * @version 2.0
 */
public class MonitoringService {

    private static MonitoringService instance;
    private DeviceDAO deviceDAO = new DeviceDAO();
    private NetworkMetricDAO metricDAO = new NetworkMetricDAO();
    private Random random = new Random();

    // Real data collection mode
    private boolean useRealData = true; // Default: use real data
    private Map<Integer, String> deviceInterfaceMap; // Maps device_id to network interface name

    // Cache for ping results to avoid blocking bandwidth updates
    private static class CachedPing {
        long latencyMs;
        double packetLossPct;
        long lastCheckTime;
    }
    private java.util.Map<String, CachedPing> pingCache = new java.util.concurrent.ConcurrentHashMap<>();

    // Live in-memory metric cache for instant UI reads (no DB round-trip)
    private volatile List<NetworkMetric> liveMetricCache = new ArrayList<>();
    private int dbWriteCounter = 0;
    private static final int DB_WRITE_INTERVAL = 10; // persist to DB every Nth collection

    private MonitoringService() {
        this.deviceInterfaceMap = new HashMap<>();
    }

    public static MonitoringService getInstance() {
        if (instance == null) {
            instance = new MonitoringService();
        }
        return instance;
    }

    /**
     * Main collection method - called by MonitoringPanel timer
     * Collects metrics for all monitored devices and triggers analysis
     *
     * @return List of collected metrics for UI display
     */
    public List<NetworkMetric> collectMetrics() {
        List<NetworkMetric> collectedMetrics = new ArrayList<>();
        List<Device> devices = deviceDAO.getAllDevices();

        if (useRealData) {
            // REAL MODE: Collect actual system network statistics
            collectedMetrics = collectRealMetrics(devices);
        } else {
            // SIMULATED MODE: Generate random metrics for demo
            collectedMetrics = collectSimulatedMetrics(devices);
        }

        // Always update live cache for instant UI reads
        liveMetricCache = collectedMetrics;
        
        // Only write to DB every Nth cycle to avoid thrashing
        dbWriteCounter++;
        boolean shouldPersist = (dbWriteCounter >= DB_WRITE_INTERVAL);
        if (shouldPersist) {
            dbWriteCounter = 0;
        }

        // Process each collected metric
        for (NetworkMetric metric : collectedMetrics) {
            // 1. Save to database (only periodically)
            if (shouldPersist) {
                metricDAO.insertMetric(metric);
            }

            // 2. Update device status based on metric
            String status = determineDeviceStatus(metric);
            deviceDAO.updateDeviceStatus(metric.getDeviceId(), status);

            // 3. Trigger alert threshold checking (only on DB writes to avoid alert flood)
            if (shouldPersist) {
                try {
                    AlertService alertService = AlertService.getInstance();
                    alertService.checkThresholds(metric);
                } catch (Exception e) {
                    System.err.println("[MonitoringService] Alert check error: " + e.getMessage());
                }

                // 4. Trigger security threat analysis
                try {
                    SecurityService securityService = SecurityService.getInstance();
                    securityService.analyze(metric);
                } catch (Exception e) {
                    System.err.println("[MonitoringService] Security analysis error: " + e.getMessage());
                }
            }
        }

        System.out.println("[MonitoringService] Collected " +
            (useRealData ? "REAL" : "SIMULATED") +
            " metrics from " + collectedMetrics.size() + " devices" +
            (shouldPersist ? " [DB WRITE]" : " [CACHE ONLY]"));

        return collectedMetrics;
    }

    /**
     * Collect REAL network metrics from system interfaces
     * Maps system network interfaces to monitored devices
     */
    private List<NetworkMetric> collectRealMetrics(List<Device> devices) {
        // Get all system network statistics
        SystemNetworkStats systemStats = NetworkAdapter.getAllNetworkStats();

        List<NetworkMetric> metrics = devices.parallelStream().map(device -> {
            try {
                NetworkMetric metric = new NetworkMetric();
                metric.setDeviceId(device.getDeviceId());

                // Try to find matching network interface for this device
                InterfaceStats ifStats = findMatchingInterface(device, systemStats);

                if (ifStats != null) {
                    // We found a matching interface - use real data
                    // Calculate real bandwidth in Mbps directly from bytes per sec
                    double bandwidthMbps = (ifStats.rxBytesPerSec + ifStats.txBytesPerSec) * 8.0 / 1_000_000.0;
                    
                    metric.setBandwidthUsage(bandwidthMbps);
                    metric.setPacketsIn(ifStats.packetsReceived);
                    metric.setPacketsOut(ifStats.packetsSent);
                } else {
                    metric.setBandwidthUsage(0);
                    metric.setPacketsIn(0);
                    metric.setPacketsOut(0);
                }

                // Retrieve ping metrics from cache or update them if older than 3000ms
                long now = System.currentTimeMillis();
                CachedPing cached = pingCache.get(device.getIpAddress());
                if (cached == null || now - cached.lastCheckTime > 3000) {
                    if (cached == null) {
                        cached = new CachedPing();
                        cached.latencyMs = 0;
                        cached.packetLossPct = 0.0;
                        cached.lastCheckTime = now;
                        pingCache.put(device.getIpAddress(), cached);
                    }
                    
                    // Mark as checking immediately to prevent multiple threads queuing the same ping
                    cached.lastCheckTime = now;
                    
                    final CachedPing finalCached = cached;
                    final String ipAddr = device.getIpAddress();
                    
                    // Offload slow ICMP pings to a background thread so UI doesn't freeze
                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        boolean reachable = NetworkAdapter.isReachable(ipAddr, 500);
                        if (reachable) {
                            long latency = NetworkAdapter.getLatencyMs(ipAddr);
                            finalCached.latencyMs = latency >= 0 ? latency : 0;
                            finalCached.packetLossPct = NetworkAdapter.getPacketLossPercent(ipAddr, 2);
                        } else {
                            finalCached.latencyMs = 9999;
                            finalCached.packetLossPct = 100.0;
                        }
                    });
                }

                metric.setLatencyMs(cached.latencyMs);
                metric.setPacketLossPct(cached.packetLossPct);

                return metric;
            } catch (Exception e) {
                System.err.println("[MonitoringService] Error collecting real metric for device " +
                    device.getDeviceId() + ": " + e.getMessage());
                return null;
            }
        }).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toList());

        return metrics;
    }

    /**
     * Collect SIMULATED metrics (original behavior for demo/testing)
     */
    private List<NetworkMetric> collectSimulatedMetrics(List<Device> devices) {
        List<NetworkMetric> metrics = new ArrayList<>();

        for (Device device : devices) {
            try {
                // Check if device is reachable
                boolean isReachable = isDeviceReachable(device.getIpAddress());

                // Generate metric data
                NetworkMetric metric = generateSimulatedMetric(device.getDeviceId(), isReachable);
                metrics.add(metric);

                // Update device status
                String status = isReachable ? "ONLINE" : "OFFLINE";
                deviceDAO.updateDeviceStatus(device.getDeviceId(), status);

            } catch (Exception e) {
                System.err.println("[MonitoringService] Error simulating metric for device " +
                    device.getDeviceId() + ": " + e.getMessage());
            }
        }

        return metrics;
    }

    /**
     * Find network interface that matches a monitored device
     * Matching strategies:
     * 1. Manual mapping from deviceInterfaceMap
     * 2. IP address exact match
     * 3. Device name contains interface name
     * 4. Default to first active interface if device is "server"/"main"/"system"
     */
    private InterfaceStats findMatchingInterface(Device device, SystemNetworkStats systemStats) {
        String deviceIP = device.getIpAddress();
        String deviceName = device.getDeviceName().toLowerCase();

        // Strategy 1: Manual mapping (highest priority)
        if (deviceInterfaceMap.containsKey(device.getDeviceId())) {
            String mappedInterface = deviceInterfaceMap.get(device.getDeviceId());
            for (InterfaceStats ifStats : systemStats.interfaces) {
                if (ifStats.name.equals(mappedInterface) ||
                    ifStats.displayName.equalsIgnoreCase(mappedInterface)) {
                    return ifStats;
                }
            }
        }

        // Strategy 2: Exact IP match
        for (InterfaceStats ifStats : systemStats.interfaces) {
            if (ifStats.ipAddress != null && ifStats.ipAddress.equals(deviceIP)) {
                return ifStats;
            }
        }

        // Strategy 3: Name contains interface name
        for (InterfaceStats ifStats : systemStats.interfaces) {
            String ifName = ifStats.name.toLowerCase();
            String ifDisplayName = ifStats.displayName != null ? ifStats.displayName.toLowerCase() : "";

            if (deviceName.contains(ifName) || deviceName.contains(ifDisplayName) ||
                ifName.contains(deviceName) || ifDisplayName.contains(deviceName)) {
                return ifStats;
            }
        }

        // Strategy 4: If this is a "system", "gateway", or "router" device, use primary interface
        // (Since all local traffic goes through the router, the PC's primary interface reflects its usage)
        if (deviceName.contains("server") || deviceName.contains("main") ||
            deviceName.contains("system") || deviceName.contains("local") ||
            deviceName.contains("gateway") || deviceName.contains("router")) {
            if (!systemStats.interfaces.isEmpty()) {
                // Find the interface with the highest traffic to ensure we pick the real active connection
                // instead of a virtual/WSL adapter which has 0 traffic.
                InterfaceStats best = systemStats.interfaces.get(0);
                long maxTraffic = best.rxBytesPerSec + best.txBytesPerSec;
                for (InterfaceStats is : systemStats.interfaces) {
                    long traffic = is.rxBytesPerSec + is.txBytesPerSec;
                    if (traffic > maxTraffic) {
                        best = is;
                        maxTraffic = traffic;
                    }
                }
                return best;
            }
        }

        // Fallback: If no match found, this is a remote LAN device (like a phone or TV).
        // We cannot measure its bandwidth natively from this host PC, so we return null
        // to fall back to ping-only metrics + simulated remote traffic.
        return null; // No match found — will fall back to ping-only metrics
    }

    /**
     * Manually map a device to a specific network interface
     * Useful when automatic matching fails
     *
     * @param deviceId Device ID from database
     * @param interfaceName System network interface name (e.g., "eth0", "wlan0", "Wi-Fi")
     */
    public void mapDeviceToInterface(int deviceId, String interfaceName) {
        deviceInterfaceMap.put(deviceId, interfaceName);
        System.out.println("[MonitoringService] Mapped device " + deviceId + " → " + interfaceName);
    }

    /**
     * Get available network interfaces (for UI dropdown/selection)
     */
    public List<String> getAvailableInterfaces() {
        List<String> interfaces = new ArrayList<>();
        SystemNetworkStats stats = NetworkAdapter.getAllNetworkStats();

        for (InterfaceStats ifStats : stats.interfaces) {
            interfaces.add(String.format("%s (%s) - %s",
                ifStats.name, ifStats.displayName, ifStats.ipAddress));
        }

        return interfaces;
    }

    /**
     * Get network summary for dashboard header
     */
    public String getNetworkSummary() {
        if (useRealData) {
            return NetworkAdapter.getNetworkSummary();
        } else {
            List<Device> devices = deviceDAO.getAllDevices();
            long onlineCount = 0;
            for (Device d : devices) {
                if ("ONLINE".equals(d.getStatus())) {
                    onlineCount++;
                }
            }
            return String.format("Mode: SIMULATED | Devices: %d | Online: %d",
                devices.size(), onlineCount);
        }
    }

    /**
     * Test connectivity to a specific IP
     */
    public boolean testConnection(String ipAddress) {
        return NetworkAdapter.isReachable(ipAddress, 5000);
    }

    /**
     * Toggle between real and simulated data collection
     */
    public void setUseRealData(boolean useRealData) {
        this.useRealData = useRealData;
        System.out.println("[MonitoringService] Data mode set to: " +
            (useRealData ? "REAL" : "SIMULATED"));
    }

    /**
     * Check current data source mode
     */
    public boolean isUsingRealData() {
        return useRealData;
    }

    /**
     * Get device status string based on latest metric
     */
    public String getDeviceStatus(int deviceId) {
        NetworkMetric latestMetric = metricDAO.getLatestByDevice(deviceId);
        if (latestMetric == null) {
            return "UNKNOWN";
        }
        return determineDeviceStatus(latestMetric);
    }

    /**
     * Determine device status based on collected metrics
     * Returns: ONLINE, OFFLINE, WARNING, or CRITICAL
     */
    private String determineDeviceStatus(NetworkMetric metric) {
        // Offline if latency is 9999 (unreachable) or packet loss is 100%
        if (metric.getLatencyMs() >= 9999 || metric.getPacketLossPct() >= 100) {
            return "OFFLINE";
        }

        // CRITICAL if very high latency or bandwidth
        if (metric.getLatencyMs() > 500 || metric.getBandwidthUsage() > 95) {
            return "CRITICAL";
        }

        // WARNING if high latency or packet loss
        if (metric.getLatencyMs() > 200 || metric.getPacketLossPct() > 5.0) {
            return "WARNING";
        }

        return "ONLINE";
    }

    // ========== Legacy Methods (backward compatible) ==========

    /**
     * Get latest metrics — use live cache for instant reads, fall back to DB
     */
    public List<NetworkMetric> getLatestMetrics() {
        List<NetworkMetric> cached = liveMetricCache;
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        return metricDAO.getLatestMetrics();
    }

    /**
     * Get latest metric for a specific device
     */
    public NetworkMetric getLatestMetricByDevice(int deviceId) {
        return metricDAO.getLatestByDevice(deviceId);
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
     * Get average bandwidth across all devices — reads from live cache
     */
    public double getAverageBandwidth() {
        List<NetworkMetric> metrics = liveMetricCache;
        if (metrics == null || metrics.isEmpty()) {
            metrics = metricDAO.getLatestMetrics();
        }
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

    // ========== Private Helpers ==========

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
     * Generate simulated metric data (for demo mode)
     */
    private NetworkMetric generateSimulatedMetric(int deviceId, boolean isOnline) {
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
            metric.setLatencyMs(9999);
            metric.setPacketLossPct(100);
            metric.setPacketsIn(0);
            metric.setPacketsOut(0);
        }

        return metric;
    }

    /**
     * Generate simulated traffic for a specific device (public API)
     */
    public NetworkMetric simulateTraffic(int deviceId) {
        return generateSimulatedMetric(deviceId, true);
    }

    /**
     * Convert bytes to Mbps (assuming 10-second collection interval)
     */
    private double convertBytesToMbps(long bytes) {
        // bytes * 8 bits/byte / 1,000,000 bits/Mbps / 10 seconds
        return (bytes * 8.0) / 1_000_000.0 / 10.0;
    }
}
