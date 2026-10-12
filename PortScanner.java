package com.networkmonitor.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.*;

/**
 * PortScanner — High-performance concurrent TCP port scanner and service banner analyzer.
 */
public class PortScanner {

    public static class PortScanResult {
        private final int port;
        private final String serviceName;
        private final String status; // "OPEN", "CLOSED", "FILTERED"
        private final long responseTimeMs;
        private final String banner;

        public PortScanResult(int port, String serviceName, String status, long responseTimeMs, String banner) {
            this.port = port;
            this.serviceName = serviceName;
            this.status = status;
            this.responseTimeMs = responseTimeMs;
            this.banner = (banner != null && !banner.isEmpty()) ? banner : "-";
        }

        public int getPort() { return port; }
        public String getServiceName() { return serviceName; }
        public String getStatus() { return status; }
        public long getResponseTimeMs() { return responseTimeMs; }
        public String getBanner() { return banner; }
    }

    public interface PortScanListener {
        void onPortScanned(PortScanResult result);
        void onProgress(int completed, int total);
        void onComplete(List<PortScanResult> results);
    }

    public static final int[] PRESET_COMMON = {
        21, 22, 23, 25, 53, 80, 110, 135, 139, 143, 443, 445, 1433, 1521, 3306, 3389, 5432, 6379, 8080, 8443, 27017
    };

    public static final int[] PRESET_WEB = {
        80, 443, 8000, 8080, 8443, 8888, 9000
    };

    public static final int[] PRESET_DATABASE = {
        1433, 1521, 3306, 5432, 6379, 27017
    };

    public static final int[] PRESET_ADMIN = {
        22, 23, 135, 139, 445, 3389, 5900
    };

    private static final Map<Integer, String> WELL_KNOWN_SERVICES = new HashMap<>();
    static {
        WELL_KNOWN_SERVICES.put(21, "FTP");
        WELL_KNOWN_SERVICES.put(22, "SSH");
        WELL_KNOWN_SERVICES.put(23, "Telnet");
        WELL_KNOWN_SERVICES.put(25, "SMTP");
        WELL_KNOWN_SERVICES.put(53, "DNS");
        WELL_KNOWN_SERVICES.put(80, "HTTP");
        WELL_KNOWN_SERVICES.put(110, "POP3");
        WELL_KNOWN_SERVICES.put(135, "MS-RPC");
        WELL_KNOWN_SERVICES.put(139, "NetBIOS");
        WELL_KNOWN_SERVICES.put(143, "IMAP");
        WELL_KNOWN_SERVICES.put(443, "HTTPS");
        WELL_KNOWN_SERVICES.put(445, "SMB");
        WELL_KNOWN_SERVICES.put(1433, "MS-SQL");
        WELL_KNOWN_SERVICES.put(1521, "Oracle DB");
        WELL_KNOWN_SERVICES.put(3306, "MySQL");
        WELL_KNOWN_SERVICES.put(3389, "RDP");
        WELL_KNOWN_SERVICES.put(5432, "PostgreSQL");
        WELL_KNOWN_SERVICES.put(5900, "VNC");
        WELL_KNOWN_SERVICES.put(6379, "Redis");
        WELL_KNOWN_SERVICES.put(8000, "HTTP-Dev");
        WELL_KNOWN_SERVICES.put(8080, "HTTP-Proxy");
        WELL_KNOWN_SERVICES.put(8443, "HTTPS-Alt");
        WELL_KNOWN_SERVICES.put(8888, "HTTP-Alt");
        WELL_KNOWN_SERVICES.put(9000, "FastCGI / Sonar");
        WELL_KNOWN_SERVICES.put(27017, "MongoDB");
    }

    public static String getServiceName(int port) {
        return WELL_KNOWN_SERVICES.getOrDefault(port, "Custom (" + port + ")");
    }

    /**
     * Executes multi-threaded TCP scan asynchronously.
     */
    public static Future<?> scanTargetAsync(String hostIp, int[] ports, int timeoutMs,
                                            PortScanListener listener, ExecutorService pool) {
        ExecutorService executor = (pool != null) ? pool : Executors.newFixedThreadPool(16);

        return CompletableFuture.runAsync(() -> {
            List<PortScanResult> allResults = Collections.synchronizedList(new ArrayList<>());
            int total = ports.length;
            CountDownLatch latch = new CountDownLatch(total);
            java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger(0);

            for (int port : ports) {
                executor.submit(() -> {
                    PortScanResult res = scanSinglePort(hostIp, port, timeoutMs);
                    allResults.add(res);
                    if (listener != null) {
                        listener.onPortScanned(res);
                    }
                    int done = completed.incrementAndGet();
                    if (listener != null) {
                        listener.onProgress(done, total);
                    }
                    latch.countDown();
                });
            }

            try {
                latch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}

            allResults.sort(Comparator.comparingInt(PortScanResult::getPort));

            if (listener != null) {
                listener.onComplete(allResults);
            }
        });
    }

    public static PortScanResult scanSinglePort(String hostIp, int port, int timeoutMs) {
        long start = System.currentTimeMillis();
        String service = getServiceName(port);
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(Math.min(timeoutMs, 600));
            socket.connect(new InetSocketAddress(hostIp, port), timeoutMs);
            long elapsed = System.currentTimeMillis() - start;

            // Probe banner if HTTP or text protocol
            String banner = tryReadBanner(socket, port);
            return new PortScanResult(port, service, "OPEN", Math.max(1, elapsed), banner);
        } catch (SocketTimeoutException e) {
            long elapsed = System.currentTimeMillis() - start;
            return new PortScanResult(port, service, "FILTERED", elapsed, "Timed out (> " + timeoutMs + "ms)");
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("refused") || msg.contains("reset")) {
                return new PortScanResult(port, service, "CLOSED", Math.max(1, elapsed), "Port Closed / RST");
            }
            return new PortScanResult(port, service, "FILTERED", elapsed, "Unreachable");
        }
    }

    private static String tryReadBanner(Socket socket, int port) {
        try {
            if (port == 80 || port == 8080 || port == 8000) {
                socket.getOutputStream().write("HEAD / HTTP/1.0\r\n\r\n".getBytes());
                socket.getOutputStream().flush();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.toLowerCase().startsWith("server:")) {
                        return line.substring(7).trim();
                    }
                }
            } else if (port == 21 || port == 22 || port == 25 || port == 110) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    return line.trim();
                }
            }
        } catch (Exception ignored) {}
        return "-";
    }
}
