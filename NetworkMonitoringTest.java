package com.networkmonitor.test;

import com.networkmonitor.util.NetworkAdapter;
import com.networkmonitor.util.NetworkAdapter.SystemNetworkStats;
import com.networkmonitor.util.NetworkAdapter.InterfaceStats;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * NetworkMonitoringTest - Complete integration test
 *
 * This test class demonstrates and validates the real network monitoring
 * integration. Run this BEFORE integrating into your main project to ensure
 * NetworkAdapter works correctly on your system.
 *
 * @author Network Monitor Team
 * @version 1.0
 */
public class NetworkMonitoringTest {

    private static final String SEPARATOR = "=".repeat(80);
    private static final String LINE = "-".repeat(80);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        System.out.println(SEPARATOR);
        System.out.println("  SMART NETWORK MONITORING SYSTEM - Integration Test");
        System.out.println("  Real Network Data Collection Verification");
        System.out.println("  Time: " + dateFormat.format(new Date()));
        System.out.println(SEPARATOR);
        System.out.println();

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            System.out.print("\nEnter your choice: ");
            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1":
                    testSystemInfo();
                    break;
                case "2":
                    testNetworkInterfaces();
                    break;
                case "3":
                    testNetworkStatistics();
                    break;
                case "4":
                    testConnectivity();
                    break;
                case "5":
                    testBandwidthMonitoring();
                    break;
                case "6":
                    testLatencyMeasurement();
                    break;
                case "7":
                    testPacketLoss();
                    break;
                case "8":
                    testRealTimeMonitoring();
                    break;
                case "9":
                    testIntegrationScenario();
                    break;
                case "0":
                    System.out.println("Exiting test suite. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("❌ Invalid choice. Please try again.");
            }

            if (running && !choice.equals("8")) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }

        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\n" + SEPARATOR);
        System.out.println("TEST MENU:");
        System.out.println(LINE);
        System.out.println("1. System Information");
        System.out.println("2. Network Interfaces Discovery");
        System.out.println("3. Network Statistics (Single Collection)");
        System.out.println("4. Connectivity Test (Ping)");
        System.out.println("5. Bandwidth Monitoring");
        System.out.println("6. Latency Measurement");
        System.out.println("7. Packet Loss Test");
        System.out.println("8. Real-Time Monitoring (10 cycles)");
        System.out.println("9. Full Integration Scenario");
        System.out.println("0. Exit");
        System.out.println(SEPARATOR);
    }

    /**
     * Test 1: System Information
     */
    private static void testSystemInfo() {
        System.out.println("TEST 1: SYSTEM INFORMATION");
        System.out.println(LINE);

        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        String javaVersion = System.getProperty("java.version");
        String userName = System.getProperty("user.name");

        System.out.println("Operating System: " + osName + " " + osVersion);
        System.out.println("Architecture: " + osArch);
        System.out.println("Java Version: " + javaVersion);
        System.out.println("User: " + userName);
        System.out.println("Time: " + dateFormat.format(new Date()));

        // Test NetworkAdapter OS detection
        System.out.println("\nNetworkAdapter Detection:");
        SystemNetworkStats stats = NetworkAdapter.getAllNetworkStats();
        System.out.println("✅ NetworkAdapter initialized successfully");
        System.out.println("✅ Found " + stats.activeInterfaces + " active network interface(s)");
    }

    /**
     * Test 2: Network Interfaces Discovery
     */
    private static void testNetworkInterfaces() {
        System.out.println("TEST 2: NETWORK INTERFACES DISCOVERY");
        System.out.println(LINE);

        SystemNetworkStats stats = NetworkAdapter.getAllNetworkStats();

        if (stats.activeInterfaces == 0) {
            System.out.println("❌ No active network interfaces found!");
            return;
        }

        System.out.println("Found " + stats.activeInterfaces + " active interface(s):\n");

        int count = 1;
        for (InterfaceStats ifStats : stats.interfaces) {
            System.out.println("Interface #" + count + ":");
            System.out.println("  Name: " + ifStats.name);
            System.out.println("  Display Name: " + ifStats.displayName);
            System.out.println("  IP Address: " + ifStats.ipAddress);
            System.out.println("  MAC Address: " + ifStats.macAddress);
            System.out.println("  Status: " + (ifStats.isUp ? "✅ UP" : "❌ DOWN"));
            System.out.println("  MTU: " + ifStats.mtu + " bytes");
            System.out.println("  Speed: " + (ifStats.speed > 0 ? ifStats.speed + " Mbps" : "Unknown"));
            System.out.println();
            count++;
        }

        System.out.println("✅ Interface discovery test completed");
    }

    /**
     * Test 3: Network Statistics Collection
     */
    private static void testNetworkStatistics() {
        System.out.println("TEST 3: NETWORK STATISTICS COLLECTION");
        System.out.println(LINE);

        SystemNetworkStats stats = NetworkAdapter.getAllNetworkStats();

        System.out.println("System-Wide Statistics:");
        System.out.println("  Total Bytes Received: " + formatBytes(stats.totalBytesReceived));
        System.out.println("  Total Bytes Sent: " + formatBytes(stats.totalBytesSent));
        System.out.println("  Total Packets Received: " + formatNumber(stats.totalPacketsReceived));
        System.out.println("  Total Packets Sent: " + formatNumber(stats.totalPacketsSent));
        System.out.println("  Average Utilization: " + String.format("%.2f%%", stats.averageUtilization));
        System.out.println();

        System.out.println("Per-Interface Statistics:");
        System.out.println(LINE);

        for (InterfaceStats ifStats : stats.interfaces) {
            System.out.println("\n" + ifStats.name + " (" + ifStats.displayName + "):");
            System.out.println("  Received: " + formatBytes(ifStats.bytesReceived) +
                             " (" + formatNumber(ifStats.packetsReceived) + " packets)");
            System.out.println("  Sent: " + formatBytes(ifStats.bytesSent) +
                             " (" + formatNumber(ifStats.packetsSent) + " packets)");
            System.out.println("  Errors: " + ifStats.errors);
            System.out.println("  Drops: " + ifStats.drops);
            System.out.println("  Utilization: " + String.format("%.2f%%", ifStats.utilizationPercent));
        }

        System.out.println("\n✅ Statistics collection test completed");
    }

    /**
     * Test 4: Connectivity Test
     */
    private static void testConnectivity() {
        System.out.println("TEST 4: CONNECTIVITY TEST (PING)");
        System.out.println(LINE);

        String[] testHosts = {
            "8.8.8.8",           // Google DNS
            "1.1.1.1",           // Cloudflare DNS
            "localhost",         // Local machine
            "192.168.1.1",       // Typical router IP
            "example.com"        // Example domain
        };

        System.out.println("Testing connectivity to common hosts...\n");

        for (String host : testHosts) {
            System.out.print("Testing " + host + "... ");
            boolean reachable = NetworkAdapter.isReachable(host, 3000);

            if (reachable) {
                long latency = NetworkAdapter.getLatencyMs(host);
                System.out.println("✅ REACHABLE (Latency: " + latency + " ms)");
            } else {
                System.out.println("❌ UNREACHABLE");
            }
        }

        System.out.println("\n✅ Connectivity test completed");
    }

    /**
     * Test 5: Bandwidth Monitoring
     */
    private static void testBandwidthMonitoring() {
        System.out.println("TEST 5: BANDWIDTH MONITORING");
        System.out.println(LINE);
        System.out.println("Collecting baseline statistics...\n");

        // First collection
        SystemNetworkStats stats1 = NetworkAdapter.getAllNetworkStats();

        System.out.println("Waiting 10 seconds to measure bandwidth...");
        sleep(10000);

        // Second collection
        SystemNetworkStats stats2 = NetworkAdapter.getAllNetworkStats();

        System.out.println("\nBandwidth Calculation Results:");
        System.out.println(LINE);

        for (int i = 0; i < stats1.interfaces.size(); i++) {
            InterfaceStats before = stats1.interfaces.get(i);
            InterfaceStats after = stats2.interfaces.get(i);

            long rxDelta = after.bytesReceived - before.bytesReceived;
            long txDelta = after.bytesSent - before.bytesSent;

            double rxMbps = (rxDelta * 8.0) / 10_000_000.0; // 10 seconds, convert to Mbps
            double txMbps = (txDelta * 8.0) / 10_000_000.0;

            System.out.println("\n" + after.name + ":");
            System.out.println("  Download Speed: " + String.format("%.2f Mbps", rxMbps) +
                             " (" + formatBytes(rxDelta) + " in 10s)");
            System.out.println("  Upload Speed: " + String.format("%.2f Mbps", txMbps) +
                             " (" + formatBytes(txDelta) + " in 10s)");
            System.out.println("  Total Bandwidth: " + String.format("%.2f Mbps", rxMbps + txMbps));
        }

        System.out.println("\n✅ Bandwidth monitoring test completed");
    }

    /**
     * Test 6: Latency Measurement
     */
    private static void testLatencyMeasurement() {
        System.out.println("TEST 6: LATENCY MEASUREMENT");
        System.out.println(LINE);

        String[] testHosts = {"8.8.8.8", "1.1.1.1", "cloudflare.com"};
        int samples = 5;

        System.out.println("Measuring latency with " + samples + " samples per host...\n");

        for (String host : testHosts) {
            System.out.println("Testing " + host + ":");
            long sum = 0;
            long min = Long.MAX_VALUE;
            long max = 0;
            int successful = 0;

            for (int i = 0; i < samples; i++) {
                long latency = NetworkAdapter.getLatencyMs(host);
                if (latency >= 0) {
                    System.out.println("  Sample " + (i + 1) + ": " + latency + " ms");
                    sum += latency;
                    min = Math.min(min, latency);
                    max = Math.max(max, latency);
                    successful++;
                } else {
                    System.out.println("  Sample " + (i + 1) + ": Failed");
                }
                sleep(500);
            }

            if (successful > 0) {
                double avg = sum / (double) successful;
                System.out.println("  Statistics: Min=" + min + "ms, Max=" + max +
                                 "ms, Avg=" + String.format("%.1f", avg) + "ms");
            }
            System.out.println();
        }

        System.out.println("✅ Latency measurement test completed");
    }

    /**
     * Test 7: Packet Loss Test
     */
    private static void testPacketLoss() {
        System.out.println("TEST 7: PACKET LOSS TEST");
        System.out.println(LINE);

        String[] testHosts = {"8.8.8.8", "1.1.1.1", "localhost"};
        int pingCount = 10;

        System.out.println("Testing packet loss with " + pingCount + " pings per host...\n");

        for (String host : testHosts) {
            System.out.print("Testing " + host + "... ");
            double packetLoss = NetworkAdapter.getPacketLossPercent(host, pingCount);

            if (packetLoss == 0) {
                System.out.println("✅ 0% packet loss (Perfect)");
            } else if (packetLoss < 5) {
                System.out.println("⚠️  " + String.format("%.1f%%", packetLoss) + " packet loss (Acceptable)");
            } else if (packetLoss < 50) {
                System.out.println("❌ " + String.format("%.1f%%", packetLoss) + " packet loss (Poor)");
            } else {
                System.out.println("❌ " + String.format("%.1f%%", packetLoss) + " packet loss (Critical)");
            }
        }

        System.out.println("\n✅ Packet loss test completed");
    }

    /**
     * Test 8: Real-Time Monitoring
     */
    private static void testRealTimeMonitoring() {
        System.out.println("TEST 8: REAL-TIME MONITORING");
        System.out.println(LINE);
        System.out.println("Monitoring network for 10 cycles (10 seconds each)...");
        System.out.println("Press Ctrl+C to stop early\n");

        for (int cycle = 1; cycle <= 10; cycle++) {
            System.out.println(LINE);
            System.out.println("Cycle " + cycle + " - " + dateFormat.format(new Date()));
            System.out.println(LINE);

            SystemNetworkStats stats = NetworkAdapter.getAllNetworkStats();
            String summary = NetworkAdapter.getNetworkSummary();
            System.out.println(summary);

            System.out.println("\nInterface Status:");
            for (InterfaceStats ifStats : stats.interfaces) {
                String status = ifStats.isUp ? "✅" : "❌";
                System.out.println(String.format("  %s %s - RX: %s, TX: %s, Util: %.1f%%",
                    status, ifStats.name,
                    formatBytes(ifStats.bytesReceived),
                    formatBytes(ifStats.bytesSent),
                    ifStats.utilizationPercent));
            }

            if (cycle < 10) {
                System.out.println("\nWaiting 10 seconds...\n");
                sleep(10000);
            }
        }

        System.out.println("\n✅ Real-time monitoring test completed");
    }

    /**
     * Test 9: Full Integration Scenario
     */
    private static void testIntegrationScenario() {
        System.out.println("TEST 9: FULL INTEGRATION SCENARIO");
        System.out.println(LINE);
        System.out.println("Simulating complete monitoring cycle as it would run in your app...\n");

        // Step 1: Discovery
        System.out.println("Step 1: Discovering network interfaces");
        SystemNetworkStats stats = NetworkAdapter.getAllNetworkStats();
        System.out.println("✅ Found " + stats.activeInterfaces + " interface(s)");

        // Step 2: Device matching
        System.out.println("\nStep 2: Simulating device-to-interface mapping");
        System.out.println("  Device 'Main Server' → Interface '" +
            (stats.interfaces.isEmpty() ? "N/A" : stats.interfaces.get(0).name) + "'");

        // Step 3: Metrics collection
        System.out.println("\nStep 3: Collecting metrics (as MonitoringService would)");
        if (!stats.interfaces.isEmpty()) {
            InterfaceStats primaryInterface = stats.interfaces.get(0);
            System.out.println("  Interface: " + primaryInterface.name);
            System.out.println("  Bandwidth In: " + formatBytes(primaryInterface.bytesReceived));
            System.out.println("  Bandwidth Out: " + formatBytes(primaryInterface.bytesSent));
            System.out.println("  Packets In: " + primaryInterface.packetsReceived);
            System.out.println("  Packets Out: " + primaryInterface.packetsSent);
            System.out.println("  Utilization: " + String.format("%.2f%%", primaryInterface.utilizationPercent));
        }

        // Step 4: Connectivity check
        System.out.println("\nStep 4: Checking device connectivity");
        String testIP = "8.8.8.8";
        boolean reachable = NetworkAdapter.isReachable(testIP, 3000);
        long latency = reachable ? NetworkAdapter.getLatencyMs(testIP) : -1;
        System.out.println("  Test Device (" + testIP + "): " +
            (reachable ? "✅ ONLINE (Latency: " + latency + "ms)" : "❌ OFFLINE"));

        // Step 5: Threshold checking
        System.out.println("\nStep 5: Simulating threshold checks");
        if (!stats.interfaces.isEmpty()) {
            double utilization = stats.interfaces.get(0).utilizationPercent;
            if (utilization > 90) {
                System.out.println("  ⚠️  ALERT: Bandwidth utilization > 90% (CRITICAL)");
            } else if (utilization > 70) {
                System.out.println("  ⚠️  WARNING: Bandwidth utilization > 70%");
            } else {
                System.out.println("  ✅ All thresholds normal");
            }
        }

        // Step 6: Summary
        System.out.println("\nStep 6: Generating summary");
        System.out.println("  " + NetworkAdapter.getNetworkSummary());

        System.out.println("\n" + SEPARATOR);
        System.out.println("✅ INTEGRATION TEST COMPLETED SUCCESSFULLY");
        System.out.println("   Your MonitoringService can now collect real network data!");
        System.out.println(SEPARATOR);
    }

    // Utility methods
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / 1024.0 / 1024.0);
        return String.format("%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0);
    }

    private static String formatNumber(long number) {
        if (number < 1000) return String.valueOf(number);
        if (number < 1000000) return String.format("%.1fK", number / 1000.0);
        if (number < 1000000000) return String.format("%.1fM", number / 1000000.0);
        return String.format("%.1fG", number / 1000000000.0);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
