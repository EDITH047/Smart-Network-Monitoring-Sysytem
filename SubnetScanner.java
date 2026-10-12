package com.networkmonitor.service;

import com.networkmonitor.util.NetworkAdapter;
import com.networkmonitor.util.NetworkAdapter.InterfaceStats;
import com.networkmonitor.util.NetworkAdapter.SystemNetworkStats;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SubnetScanner — Fast multi-threaded active subnet IP range scanner.
 * Automatically resolves responsive LAN devices, hostnames via reverse DNS,
 * physical MAC addresses via system ARP tables, and infers device types.
 */
public class SubnetScanner {

    public static class DiscoveredHost {
        private String ipAddress;
        private String hostName;
        private String macAddress;
        private String deviceType;
        private long latencyMs;

        public DiscoveredHost(String ipAddress, String hostName, String macAddress, String deviceType, long latencyMs) {
            this.ipAddress = ipAddress;
            this.hostName = hostName;
            this.macAddress = (macAddress != null && !macAddress.isEmpty()) ? macAddress : "N/A";
            this.deviceType = deviceType;
            this.latencyMs = latencyMs;
        }

        public String getIpAddress() { return ipAddress; }
        public String getHostName() { return hostName; }
        public String getMacAddress() { return macAddress; }
        public String getDeviceType() { return deviceType; }
        public long getLatencyMs() { return latencyMs; }

        public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
        public void setHostName(String hostName) { this.hostName = hostName; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

        @Override
        public String toString() {
            return String.format("%s (%s) - %s [%d ms]", hostName, ipAddress, deviceType, latencyMs);
        }
    }

    public interface ScanProgressListener {
        void onHostDiscovered(DiscoveredHost host);
        void onProgress(int current, int total);
        void onScanCompleted(List<DiscoveredHost> results);
    }

    private static final int THREAD_POOL_SIZE = 32;
    private static final int PROBE_TIMEOUT_MS = 350;
    private static final int[] PROBE_PORTS = {80, 443, 22, 135, 445, 8080, 53};

    /**
     * Infer the default local subnet prefix (e.g. "192.168.1.").
     */
    public static String detectLocalSubnetPrefix() {
        try {
            SystemNetworkStats stats = NetworkAdapter.getAllNetworkStats();
            for (InterfaceStats iface : stats.interfaces) {
                if (iface.ipAddress != null && !iface.ipAddress.startsWith("127.") && !iface.ipAddress.startsWith("169.254")) {
                    int lastDot = iface.ipAddress.lastIndexOf('.');
                    if (lastDot > 0) {
                        return iface.ipAddress.substring(0, lastDot + 1);
                    }
                }
            }
        } catch (Exception ignored) {}
        return "192.168.1.";
    }

    /**
     * Executes asynchronous subnet sweep from startHost to endHost.
     */
    public static Future<?> scanSubnetAsync(String subnetPrefix, int startHost, int endHost,
                                            ScanProgressListener listener, ExecutorService poolToUse) {
        ExecutorService executor = (poolToUse != null) ? poolToUse : Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        
        return CompletableFuture.runAsync(() -> {
            List<DiscoveredHost> discovered = Collections.synchronizedList(new ArrayList<>());
            Map<String, String> arpTable = loadArpTable();
            int total = (endHost - startHost + 1);
            CountDownLatch latch = new CountDownLatch(total);
            java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);

            for (int i = startHost; i <= endHost; i++) {
                final String targetIp = subnetPrefix + i;
                executor.submit(() -> {
                    try {
                        long start = System.currentTimeMillis();
                        boolean reachable = checkHostAlive(targetIp, PROBE_TIMEOUT_MS);
                        long latency = System.currentTimeMillis() - start;

                        if (reachable) {
                            String hostName = resolveHostName(targetIp);
                            String mac = arpTable.getOrDefault(targetIp.toLowerCase(), "N/A");
                            String devType = inferDeviceType(hostName, targetIp);
                            DiscoveredHost host = new DiscoveredHost(targetIp, hostName, mac, devType, Math.max(1, latency));
                            discovered.add(host);
                            if (listener != null) {
                                listener.onHostDiscovered(host);
                            }
                        }
                    } catch (Exception ignored) {
                    } finally {
                        int current = counter.incrementAndGet();
                        if (listener != null) {
                            listener.onProgress(current, total);
                        }
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await(60, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}

            // Secondary ARP sweep to populate any freshly resolved MACs
            Map<String, String> refreshedArp = loadArpTable();
            for (DiscoveredHost h : discovered) {
                if ("N/A".equals(h.getMacAddress())) {
                    String mac = refreshedArp.get(h.getIpAddress().toLowerCase());
                    if (mac != null) h.setMacAddress(mac);
                }
            }

            // Sort by IP numerically
            discovered.sort(Comparator.comparingInt(SubnetScanner::ipToNumber));

            if (listener != null) {
                listener.onScanCompleted(discovered);
            }
        });
    }

    /**
     * Checks if host is alive via ICMP ping and fast TCP socket probing.
     */
    public static boolean checkHostAlive(String ip, int timeoutMs) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            if (addr.isReachable(timeoutMs)) {
                return true;
            }
        } catch (Exception ignored) {}

        // Fast fallback TCP connection probes to common ports
        for (int port : PROBE_PORTS) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, port), timeoutMs);
                return true;
            } catch (Exception e) {
                // If connection refused, the host is definitely alive and rejected the port!
                String msg = e.getMessage();
                if (msg != null && (msg.toLowerCase().contains("refused") || msg.toLowerCase().contains("reset"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String resolveHostName(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            String canonical = addr.getCanonicalHostName();
            if (canonical != null && !canonical.equals(ip)) {
                return canonical;
            }
            String host = addr.getHostName();
            if (host != null && !host.equals(ip)) {
                return host;
            }
        } catch (Exception ignored) {}
        return ip;
    }

    private static String inferDeviceType(String hostname, String ip) {
        String lower = hostname.toLowerCase();
        if (ip.endsWith(".1") || ip.endsWith(".254") || lower.contains("router") || lower.contains("gateway")) {
            return "router";
        }
        if (lower.contains("switch") || lower.contains("ap") || lower.contains("unifi")) {
            return "switch";
        }
        if (lower.contains("print") || lower.contains("hp") || lower.contains("epson") || lower.contains("canon") || lower.contains("brother")) {
            return "printer";
        }
        if (lower.contains("server") || lower.contains("nas") || lower.contains("synology") || lower.contains("qnap") || lower.contains("storage")) {
            return "server";
        }
        if (lower.contains("phone") || lower.contains("android") || lower.contains("iphone") || lower.contains("ipad") || lower.contains("galaxy")) {
            return "mobile";
        }
        return "pc";
    }

    /**
     * Parses system ARP table (arp -a) on Windows/Linux to map IP -> MAC address.
     */
    public static Map<String, String> loadArpTable() {
        Map<String, String> map = new HashMap<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("arp", "-a");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                Pattern pattern = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\s+([0-9a-fA-F-]{17}|[0-9a-fA-F:]{17})");
                while ((line = reader.readLine()) != null) {
                    Matcher m = pattern.matcher(line);
                    if (m.find()) {
                        String ip = m.group(1).trim().toLowerCase();
                        String mac = m.group(2).trim().toUpperCase().replace('-', ':');
                        map.put(ip, mac);
                    }
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
        return map;
    }

    private static int ipToNumber(DiscoveredHost host) {
        try {
            String[] parts = host.getIpAddress().split("\\.");
            return (Integer.parseInt(parts[0]) << 24) |
                   (Integer.parseInt(parts[1]) << 16) |
                   (Integer.parseInt(parts[2]) << 8) |
                    Integer.parseInt(parts[3]);
        } catch (Exception e) {
            return 0;
        }
    }
}
