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
 */
public class MonitoringService {

    private static final MonitoringService instance = new MonitoringService();
    private DeviceCache deviceCache = DeviceCache.getInstance();
    private DeviceDAO deviceDAO = new DeviceDAO(); // kept for historical reasons/specific calls
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

    // Dedicated ping thread pool
    private final java.util.concurrent.ExecutorService pingExecutor =
        java.util.concurrent.Executors.newFixedThreadPool(4,
            r -> { Thread t = new Thread(r, "PingWorker"); t.setDaemon(true); return t; });

    // Live in-memory metric cache for instant UI reads (no DB round-trip)
    private volatile List<NetworkMetric> liveMetricCache = new ArrayList<>();
    private int analysisCounter = 0;

    private MonitoringService() {
        this.deviceInterfaceMap = new HashMap<>();
    }

    public static MonitoringService getInstance() {
        return instance;
    }

    /**
     * Main collection method - called by MonitoringPanel timer
     * Collects metrics for all monitored devices in-memory
     */
    public List<NetworkMetric> collectMetrics() {
        List<Device> devices = deviceCache.getAllDevices(); // RAM, not DB
        List<NetworkMetric> collectedMetrics;

        if (useRealData) {
            collectedMetrics = collectRealMetrics(devices);
        } else {
            collectedMetrics = collectSimulatedMetrics(devices);
        }

        // Always update live cache for instant UI reads
        liveMetricCache = collectedMetrics;

        // Update device statuses in-memory only
        for (NetworkMetric metric : collectedMetrics) {
            String status = determineDeviceStatus(metric);
            deviceCache.updateStatusInMemory(metric.getDeviceId(), status);
        }

        // Run Security & Alert analysis every 5 cycles (2.5 seconds at 500ms intervals) to ensure
        // real-time threat detection without spamming the database
        analysisCounter++;
        if (analysisCounter >= 5) {
            analysisCounter = 0;
            for (NetworkMetric metric : collectedMetrics) {
                try { AlertService.getInstance().checkThresholds(metric); } catch (Exception e) {}
                try { SecurityService.getInstance().analyze(metric); } catch (Exception e) {}
            }
        }

        System.out.println("[MonitoringService] Collected " +
            (useRealData ? "REAL" : "SIMULATED") +
            " metrics from " + collectedMetrics.size() + " devices [CACHE ONLY]");

        return collectedMetrics;
    }

    /**
     * DB writes, called every 30s by persistenceExecutor
     */
    public void persistMetrics() {
        List<NetworkMetric> metrics = liveMetricCache;
        if (metrics == null || metrics.isEmpty()) return;

        // 1. Batch insert metrics to DB
        for (NetworkMetric metric : metrics) {
            metricDAO.insertMetric(metric);
        }

        // 2. Flush ONLY changed device statuses to DB (dirty-tracking)
        deviceCache.flushDirtyStatuses();

        System.out.println("[MonitoringService] Persisted " + metrics.size() + " metrics [DB WRITE]");
    }

    private List<NetworkMetric> collectRealMetrics(List<Device> devices) {
        // Get all system network statistics
        SystemNetworkStats systemStats = NetworkAdapter.getAllNetworkStats();
        List<NetworkMetric> metrics = new ArrayList<>();

        for (Device device : devices) {
            try {
                NetworkMetric metric = new NetworkMetric();
                metric.setDeviceId(device.getDeviceId());

                // Try to find matching network interface for this device
                InterfaceStats ifStats = findMatchingInterface(device, systemStats);

                if (ifStats != null) {
                    double bandwidthMbps = (ifStats.rxBytesPerSec + ifStats.txBytesPerSec) * 8.0 / 1_000_000.0;
                    metric.setBandwidthUsage(bandwidthMbps);
                    metric.setPacketsIn(ifStats.packetsReceived);
                    metric.setPacketsOut(ifStats.packetsSent);
                } else {
                    metric.setBandwidthUsage(0);
                    metric.setPacketsIn(0);
                    metric.setPacketsOut(0);
                }

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
                    }, pingExecutor);
                }

                metric.setLatencyMs(cached.latencyMs);
                metric.setPacketLossPct(cached.packetLossPct);

                metrics.add(metric);
            } catch (Exception e) {
                System.err.println("[MonitoringService] Error collecting real metric for device " +
                    device.getDeviceId() + ": " + e.getMessage());
            }
        }

        return metrics;
    }

    private List<NetworkMetric> collectSimulatedMetrics(List<Device> devices) {
        List<NetworkMetric> metrics = new ArrayList<>();

        for (Device device : devices) {
            try {
                boolean isReachable = isDeviceReachable(device.getIpAddress());
                NetworkMetric metric = generateSimulatedMetric(device.getDeviceId(), isReachable);
                metrics.add(metric);
            } catch (Exception e) {
                System.err.println("[MonitoringService] Error simulating metric for device " +
                    device.getDeviceId() + ": " + e.getMessage());
            }
        }
        return metrics;
    }

    private InterfaceStats findMatchingInterface(Device device, SystemNetworkStats systemStats) {
        String deviceIP = device.getIpAddress();
        String deviceName = device.getDeviceName().toLowerCase();

        if (deviceInterfaceMap.containsKey(device.getDeviceId())) {
            String mappedInterface = deviceInterfaceMap.get(device.getDeviceId());
            for (InterfaceStats ifStats : systemStats.interfaces) {
                if (ifStats.name.equals(mappedInterface) ||
                    ifStats.displayName.equalsIgnoreCase(mappedInterface)) {
                    return ifStats;
                }
            }
        }

        for (InterfaceStats ifStats : systemStats.interfaces) {
            if (ifStats.ipAddress != null && ifStats.ipAddress.equals(deviceIP)) {
                return ifStats;
            }
        }

        for (InterfaceStats ifStats : systemStats.interfaces) {
            String ifName = ifStats.name.toLowerCase();
            String ifDisplayName = ifStats.displayName != null ? ifStats.displayName.toLowerCase() : "";

            if (deviceName.contains(ifName) || deviceName.contains(ifDisplayName) ||
                ifName.contains(deviceName) || ifDisplayName.contains(deviceName)) {
                return ifStats;
            }
        }

        if (deviceName.contains("server") || deviceName.contains("main") ||
            deviceName.contains("system") || deviceName.contains("local") ||
            deviceName.contains("gateway") || deviceName.contains("router")) {
            if (!systemStats.interfaces.isEmpty()) {
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

        return null;
    }

    public void mapDeviceToInterface(int deviceId, String interfaceName) {
        deviceInterfaceMap.put(deviceId, interfaceName);
        System.out.println("[MonitoringService] Mapped device " + deviceId + " → " + interfaceName);
    }

    public List<String> getAvailableInterfaces() {
        List<String> interfaces = new ArrayList<>();
        SystemNetworkStats stats = NetworkAdapter.getAllNetworkStats();
        for (InterfaceStats ifStats : stats.interfaces) {
            interfaces.add(String.format("%s (%s) - %s",
                ifStats.name, ifStats.displayName, ifStats.ipAddress));
        }
        return interfaces;
    }

    public String getNetworkSummary() {
        if (useRealData) {
            return NetworkAdapter.getNetworkSummary();
        } else {
            List<Device> devices = deviceCache.getAllDevices();
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

    public boolean testConnection(String ipAddress) {
        return NetworkAdapter.isReachable(ipAddress, 5000);
    }

    public void setUseRealData(boolean useRealData) {
        this.useRealData = useRealData;
        System.out.println("[MonitoringService] Data mode set to: " +
            (useRealData ? "REAL" : "SIMULATED"));
    }

    public boolean isUsingRealData() {
        return useRealData;
    }

    public String getDeviceStatus(int deviceId) {
        NetworkMetric latestMetric = metricDAO.getLatestByDevice(deviceId);
        if (latestMetric == null) {
            return "UNKNOWN";
        }
        return determineDeviceStatus(latestMetric);
    }

    private String determineDeviceStatus(NetworkMetric metric) {
        if (metric.getLatencyMs() >= 9999 || metric.getPacketLossPct() >= 100) {
            return "OFFLINE";
        }
        if (metric.getLatencyMs() > 500 || metric.getBandwidthUsage() > 95) {
            return "CRITICAL";
        }
        if (metric.getLatencyMs() > 200 || metric.getPacketLossPct() > 5.0) {
            return "WARNING";
        }
        return "ONLINE";
    }

    public List<NetworkMetric> getLatestMetrics() {
        List<NetworkMetric> cached = liveMetricCache;
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        return metricDAO.getLatestMetrics();
    }

    public NetworkMetric getLatestMetricByDevice(int deviceId) {
        return metricDAO.getLatestByDevice(deviceId);
    }

    public NetworkMetric getAverageMetrics(int deviceId, int hours) {
        return metricDAO.getAverageMetrics(deviceId, hours);
    }

    public double getPeakBandwidth(int deviceId, int hours) {
        return metricDAO.getPeakBandwidth(deviceId, hours);
    }

    public double getSystemHealth() {
        int totalDevices = deviceCache.getDeviceCount();
        if (totalDevices == 0) return 0;
        int onlineDevices = deviceCache.getOnlineDeviceCount();
        return (onlineDevices * 100.0) / totalDevices;
    }

    public double getAverageBandwidth() {
        List<NetworkMetric> metrics = liveMetricCache;
        if (metrics == null || metrics.isEmpty()) {
            metrics = metricDAO.getLatestMetrics();
        }
        if (metrics.isEmpty()) return 0;

        double total = 0;
        for (NetworkMetric metric : metrics) {
            total += metric.getBandwidthUsage();
        }
        return total / metrics.size();
    }

    public List<Device> getHighBandwidthDevices(double threshold) {
        List<Device> highBandwidthDevices = new ArrayList<>();
        List<NetworkMetric> metrics = getLatestMetrics();
        for (NetworkMetric metric : metrics) {
            if (metric.getBandwidthUsage() > threshold) {
                Device device = deviceCache.getDeviceById(metric.getDeviceId());
                if (device != null) highBandwidthDevices.add(device);
            }
        }
        return highBandwidthDevices;
    }

    public List<Device> getHighLatencyDevices(double threshold) {
        List<Device> highLatencyDevices = new ArrayList<>();
        List<NetworkMetric> metrics = getLatestMetrics();
        for (NetworkMetric metric : metrics) {
            if (metric.getLatencyMs() > threshold) {
                Device device = deviceCache.getDeviceById(metric.getDeviceId());
                if (device != null) highLatencyDevices.add(device);
            }
        }
        return highLatencyDevices;
    }

    private boolean isDeviceReachable(String ipAddress) {
        try {
            InetAddress inet = InetAddress.getByName(ipAddress);
            return inet.isReachable(3000);
        } catch (Exception e) {
            return false;
        }
    }

    private NetworkMetric generateSimulatedMetric(int deviceId, boolean isOnline) {
        NetworkMetric metric = new NetworkMetric();
        metric.setDeviceId(deviceId);

        if (isOnline) {
            metric.setBandwidthUsage(20 + random.nextDouble() * 70);
            metric.setLatencyMs(10 + random.nextDouble() * 80);
            metric.setPacketLossPct(random.nextDouble() * 2);
            metric.setPacketsIn(100000 + random.nextLong() % 500000);
            metric.setPacketsOut(50000 + random.nextLong() % 300000);
        } else {
            metric.setBandwidthUsage(0);
            metric.setLatencyMs(9999);
            metric.setPacketLossPct(100);
            metric.setPacketsIn(0);
            metric.setPacketsOut(0);
        }
        return metric;
    }

    public NetworkMetric simulateTraffic(int deviceId) {
        return generateSimulatedMetric(deviceId, true);
    }
}
