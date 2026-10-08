package com.networkmonitor.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NetworkAdapter - Enhanced with WiFi Network Scanning
 *
 * This utility class collects real network statistics from the system
 * and NOW includes the ability to scan for available WiFi networks
 * (both connected and disconnected).
 *
 * NEW FEATURE: scanAvailableNetworks() - Detects visible WiFi networks
 *
 * Compatible with: Windows, Linux, macOS
 *
 * @author Network Monitor Team
 * @version 2.0 - Added WiFi scanning
 */
public class NetworkAdapter {

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS_NAME.contains("win");
    private static final boolean IS_LINUX = OS_NAME.contains("nix") || OS_NAME.contains("nux");
    private static final boolean IS_MAC = OS_NAME.contains("mac");

    // Cache for previous statistics
    private static Map<String, InterfaceStats> previousStats = new HashMap<>();
    private static long lastCollectionTime = 0;

    /**
     * Represents statistics for a single network interface
     */
    public static class InterfaceStats {
        public String name;
        public String displayName;
        public String ipAddress;
        public String macAddress;
        public long bytesReceived;
        public long bytesSent;
        public long packetsReceived;
        public long packetsSent;
        public long errors;
        public long drops;
        public boolean isUp;
        public int mtu;
        public long speed; // Mbps
        public double utilizationPercent;
        public long rxBytesPerSec;
        public long txBytesPerSec;
        public boolean rateAlreadySet; // True when OS-specific method already computed accurate per-sec rates

        @Override
        public String toString() {
            return String.format("Interface[%s] IP=%s MAC=%s Up=%s RX=%d TX=%d RxRate=%d TxRate=%d",
                name, ipAddress, macAddress, isUp, bytesReceived, bytesSent, rxBytesPerSec, txBytesPerSec);
        }
    }

    /**
     * System-wide network statistics
     */
    public static class SystemNetworkStats {
        public long totalBytesReceived;
        public long totalBytesSent;
        public long totalPacketsReceived;
        public long totalPacketsSent;
        public int activeInterfaces;
        public double averageUtilization;
        public List<InterfaceStats> interfaces;

        public SystemNetworkStats() {
            interfaces = new ArrayList<>();
        }
    }

    /**
     * Represents an available WiFi network (NEW)
     */
    public static class AvailableNetwork {
        public String ssid;              // Network name
        public String bssid;             // MAC address of access point
        public int signalStrength;       // 0-100%
        public int channel;              // WiFi channel number
        public boolean isSecure;         // Has encryption?
        public boolean isConnected;      // Currently connected?
        public String securityType;      // WPA2, WPA3, Open, etc.

        @Override
        public String toString() {
            return String.format("%s %s (Signal: %d%%, Ch: %d, %s)",
                isConnected ? "[CONNECTED]" : "",
                ssid,
                signalStrength,
                channel,
                isSecure ? securityType : "Open");
        }
    }

    /**
     * Get all network interfaces with their current statistics
     */
    public static SystemNetworkStats getAllNetworkStats() {
        SystemNetworkStats stats = new SystemNetworkStats();
        long currentTime = System.currentTimeMillis();
        double timeDeltaSec = (currentTime - lastCollectionTime) / 1000.0;

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface netInterface = interfaces.nextElement();

                if (netInterface.isLoopback() || !netInterface.isUp()) {
                    continue;
                }

                InterfaceStats ifStats = collectInterfaceStats(netInterface, timeDeltaSec);
                if (ifStats != null) {
                    stats.interfaces.add(ifStats);
                    stats.activeInterfaces++;
                    stats.totalBytesReceived += ifStats.bytesReceived;
                    stats.totalBytesSent += ifStats.bytesSent;
                    stats.totalPacketsReceived += ifStats.packetsReceived;
                    stats.totalPacketsSent += ifStats.packetsSent;
                    stats.averageUtilization += ifStats.utilizationPercent;
                }
            }

            if (stats.activeInterfaces > 0) {
                stats.averageUtilization /= stats.activeInterfaces;
            }

            lastCollectionTime = currentTime;

        } catch (SocketException e) {
            System.err.println("Error collecting network statistics: " + e.getMessage());
        }

        return stats;
    }

    /**
     * Collect statistics for a single network interface
     */
    private static InterfaceStats collectInterfaceStats(NetworkInterface netInterface, double timeDelta) {
        InterfaceStats stats = new InterfaceStats();

        try {
            stats.name = netInterface.getName();
            stats.displayName = netInterface.getDisplayName();
            stats.isUp = netInterface.isUp();
            stats.mtu = netInterface.getMTU();

            // Get MAC address
            byte[] mac = netInterface.getHardwareAddress();
            if (mac != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mac.length; i++) {
                    sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                }
                stats.macAddress = sb.toString();
            } else {
                stats.macAddress = "N/A";
            }

            // Get IP address
            Enumeration<InetAddress> inetAddresses = netInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress addr = inetAddresses.nextElement();
                if (addr.getAddress().length == 4) { // IPv4
                    stats.ipAddress = addr.getHostAddress();
                    break;
                }
            }
            if (stats.ipAddress == null) {
                stats.ipAddress = "N/A";
            }

            // Collect OS-specific statistics
            collectOSSpecificStats(stats);

            // Calculate bandwidth utilization
            // Skip if OS-specific method already computed accurate per-sec rates (Bug 2 fix)
            if (!stats.rateAlreadySet && timeDelta > 0 && previousStats.containsKey(stats.name)) {
                InterfaceStats prev = previousStats.get(stats.name);
                long rxDelta = stats.bytesReceived - prev.bytesReceived;
                long txDelta = stats.bytesSent - prev.bytesSent;

                stats.rxBytesPerSec = (long) (rxDelta / timeDelta);
                stats.txBytesPerSec = (long) (txDelta / timeDelta);
            }

            // Calculate utilization percent from the per-sec rates
            if (stats.speed <= 0) stats.speed = 1000; // Default to 1Gbps if unknown
            double bitsPerSec = (stats.rxBytesPerSec + stats.txBytesPerSec) * 8.0;
            double speedBps = stats.speed * 1_000_000.0;
            stats.utilizationPercent = Math.min(100, (bitsPerSec / speedBps) * 100);

            previousStats.put(stats.name, cloneStats(stats));
            return stats;

        } catch (Exception e) {
            System.err.println("Error collecting stats for interface " + stats.name + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Collect OS-specific network statistics
     */
    private static void collectOSSpecificStats(InterfaceStats stats) {
        if (IS_WINDOWS) {
            collectWindowsStats(stats);
        } else if (IS_LINUX) {
            collectLinuxStats(stats);
        } else if (IS_MAC) {
            collectMacStats(stats);
        }
    }

    /**
     * Collect statistics on Windows using PowerShell Get-NetAdapterStatistics
     * for accurate PER-INTERFACE byte counters (replaces system-wide netstat -e).
     */
    // Per-interface previous byte counts for delta calculation
    private static java.util.concurrent.ConcurrentHashMap<String, long[]> previousWindowsPerIfStats = new java.util.concurrent.ConcurrentHashMap<>();
    // Cached per-interface rates (valid for one collection cycle)
    private static java.util.concurrent.ConcurrentHashMap<String, long[]> cachedWindowsRates = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        if (IS_WINDOWS) {
            Thread psThread = new Thread(() -> {
                long lastCheck = 0;
                while (true) {
                    try {
                        String psScript = "Get-NetAdapterStatistics | ForEach-Object { " +
                            "Write-Output ('{0}|{1}|{2}|{3}|{4}' -f $_.Name, $_.ReceivedBytes, $_.SentBytes, $_.ReceivedUnicastPackets, $_.SentUnicastPackets) " +
                            "}";
                        String output = executeCommandArray(new String[]{"powershell.exe", "-NoProfile", "-Command", psScript});
                        
                        long now = System.currentTimeMillis();
                        double timeDelta = (lastCheck > 0) ? (now - lastCheck) / 1000.0 : 1.0;
                        if (timeDelta <= 0) timeDelta = 1.0;

                        String[] lines = output.split("\n");
                        for (String line : lines) {
                            line = line.trim();
                            if (line.isEmpty()) continue;
                            String[] parts = line.split("\\|");
                            if (parts.length < 5) continue;

                            String adapterName = parts[0].trim();
                            try {
                                long rxBytes = Long.parseLong(parts[1].trim());
                                long txBytes = Long.parseLong(parts[2].trim());
                                long rxPackets = Long.parseLong(parts[3].trim());
                                long txPackets = Long.parseLong(parts[4].trim());

                                long rxRate = 0, txRate = 0;
                                if (previousWindowsPerIfStats.containsKey(adapterName)) {
                                    long[] prev = previousWindowsPerIfStats.get(adapterName);
                                    long rxDelta = rxBytes - prev[0];
                                    long txDelta = txBytes - prev[1];
                                    if (rxDelta < 0) rxDelta = 0;
                                    if (txDelta < 0) txDelta = 0;
                                    rxRate = (long)(rxDelta / timeDelta);
                                    txRate = (long)(txDelta / timeDelta);
                                }

                                previousWindowsPerIfStats.put(adapterName, new long[]{rxBytes, txBytes, rxPackets, txPackets});
                                cachedWindowsRates.put(adapterName, new long[]{rxRate, txRate, rxBytes, txBytes, rxPackets, txPackets});

                            } catch (NumberFormatException e) {
                                // Skip malformed lines
                            }
                        }
                        lastCheck = now;
                        Thread.sleep(500); // 500ms delay between PowerShell calls
                    } catch (Exception e) {
                        try { Thread.sleep(2000); } catch (Exception ex) {}
                    }
                }
            });
            psThread.setDaemon(true);
            psThread.start();
        }
    }

    private static void collectWindowsStats(InterfaceStats stats) {
        try {

            // Match this interface to its PowerShell adapter by display name.
            // Only assign real bandwidth to the PRIMARY interface (the one with an IP address).
            // Windows exposes sub-interfaces (WFP filters, QoS schedulers, NDIS layers) as
            // separate NetworkInterface instances with the same MAC but no IP. These must get 0
            // to avoid duplicate bandwidth counting across sub-interfaces.
            boolean isPrimaryInterface = stats.ipAddress != null && !"N/A".equals(stats.ipAddress);
            String matchKey = isPrimaryInterface ? findWindowsAdapterMatch(stats.displayName) : null;

            if (matchKey != null && cachedWindowsRates.containsKey(matchKey)) {
                long[] rates = cachedWindowsRates.get(matchKey);
                stats.rxBytesPerSec = rates[0];
                stats.txBytesPerSec = rates[1];
                stats.bytesReceived = rates[2];
                stats.bytesSent = rates[3];
                stats.packetsReceived = rates[4];
                stats.packetsSent = rates[5];
                stats.rateAlreadySet = true; // Prevent overwrite by generic delta logic
            } else {
                // Sub-interface or no matching adapter — report 0 bandwidth
                stats.rxBytesPerSec = 0;
                stats.txBytesPerSec = 0;
                stats.bytesReceived = 0;
                stats.bytesSent = 0;
                stats.rateAlreadySet = true;
            }

            stats.speed = 1000; // Default 1Gbps

        } catch (Exception e) {
            stats.bytesReceived = 0;
            stats.bytesSent = 0;
            stats.speed = 1000;
            stats.rateAlreadySet = true;
        }
    }

    /**
     * Match a Java NetworkInterface displayName to a PowerShell adapter Name.
     * PowerShell 'Name' (e.g., "Ethernet", "Wi-Fi") is typically contained in
     * Java's displayName (e.g., "Realtek Gaming GbE Family Controller").
     * We match by checking if the PowerShell Name appears in the display name,
     * or if the display name appears in the PowerShell Name.
     */
    private static String findWindowsAdapterMatch(String displayName) {
        if (displayName == null) return null;
        String displayLower = displayName.toLowerCase();

        // Try exact key match first (rare but possible)
        if (cachedWindowsRates.containsKey(displayName)) {
            return displayName;
        }

        // Try matching: PowerShell Name is a substring of Java displayName or vice versa
        for (String adapterName : cachedWindowsRates.keySet()) {
            String adapterLower = adapterName.toLowerCase();
            if (displayLower.contains(adapterLower) || adapterLower.contains(displayLower)) {
                return adapterName;
            }
        }

        // Heuristic: If displayName contains common keywords, try matching
        // e.g., "Realtek Gaming GbE Family Controller" → "Ethernet"
        //        "Intel(R) Wi-Fi 6E AX211 160MHz" → "Wi-Fi"
        if (displayLower.contains("wi-fi") || displayLower.contains("wifi") ||
            displayLower.contains("wireless") || displayLower.contains("wlan")) {
            if (cachedWindowsRates.containsKey("Wi-Fi")) return "Wi-Fi";
            if (cachedWindowsRates.containsKey("WiFi")) return "WiFi";
        }
        if (displayLower.contains("ethernet") || displayLower.contains("realtek") ||
            displayLower.contains("gbe") || displayLower.contains("gigabit")) {
            if (cachedWindowsRates.containsKey("Ethernet")) return "Ethernet";
        }
        if (displayLower.contains("bluetooth")) {
            if (cachedWindowsRates.containsKey("Bluetooth Network Connection")) return "Bluetooth Network Connection";
        }

        return null; // No match found
    }

    /**
     * Collect statistics on Linux
     */
    private static void collectLinuxStats(InterfaceStats stats) {
        try {
            String basePath = "/sys/class/net/" + stats.name + "/statistics/";

            stats.bytesReceived = readLongFromFile(basePath + "rx_bytes");
            stats.bytesSent = readLongFromFile(basePath + "tx_bytes");
            stats.packetsReceived = readLongFromFile(basePath + "rx_packets");
            stats.packetsSent = readLongFromFile(basePath + "tx_packets");
            stats.errors = readLongFromFile(basePath + "rx_errors") + readLongFromFile(basePath + "tx_errors");
            stats.drops = readLongFromFile(basePath + "rx_dropped") + readLongFromFile(basePath + "tx_dropped");

            stats.speed = readLongFromFile("/sys/class/net/" + stats.name + "/speed");
            if (stats.speed < 0) {
                stats.speed = 1000;
            }

        } catch (Exception e) {
            System.err.println("Error reading Linux network stats: " + e.getMessage());
        }
    }

    /**
     * Collect statistics on macOS
     */
    private static void collectMacStats(InterfaceStats stats) {
        try {
            String output = executeCommand("netstat -ibn");
            String[] lines = output.split("\n");

            for (String line : lines) {
                if (line.contains(stats.name)) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 10) {
                        stats.bytesReceived = parseLong(parts[6]);
                        stats.bytesSent = parseLong(parts[9]);
                        stats.packetsReceived = parseLong(parts[4]);
                        stats.packetsSent = parseLong(parts[7]);
                    }
                    break;
                }
            }

            stats.speed = 1000;

        } catch (Exception e) {
            System.err.println("Error reading macOS network stats: " + e.getMessage());
        }
    }

    /**
     * ✨ NEW: Scan for available WiFi networks
     * Returns list of all visible WiFi networks (connected and disconnected)
     */
    public static List<AvailableNetwork> scanAvailableNetworks() {
        if (IS_WINDOWS) {
            return scanWindowsWifi();
        } else if (IS_LINUX) {
            return scanLinuxWifi();
        } else if (IS_MAC) {
            return scanMacWifi();
        }
        return new ArrayList<>();
    }

    /**
     * Forces a hardware Wi-Fi scan via Windows Native WiFi API (wlanapi.dll).
     * Uses PowerShell -EncodedCommand to pass the C# script ENTIRELY IN MEMORY
     * — no temp files written to disk, no disk I/O overhead.
     */
    private static void forceWindowsWifiScan() {
        // Write the C# script in plain text — Java's Base64 encoder handles encoding
        String script =
            "$code = @\"\n" +
            "using System;\n" +
            "using System.Runtime.InteropServices;\n" +
            "public class NativeWifiScan {\n" +
            "    [DllImport(\"Wlanapi.dll\")]\n" +
            "    public static extern uint WlanOpenHandle(uint dwClientVersion, IntPtr pReserved, out uint pdwNegotiatedVersion, out IntPtr phClientHandle);\n" +
            "    [DllImport(\"Wlanapi.dll\")]\n" +
            "    public static extern uint WlanEnumInterfaces(IntPtr hClientHandle, IntPtr pReserved, out IntPtr ppInterfaceList);\n" +
            "    [DllImport(\"Wlanapi.dll\")]\n" +
            "    public static extern uint WlanScan(IntPtr hClientHandle, ref Guid pInterfaceGuid, IntPtr pDot11Ssid, IntPtr pIeData, IntPtr pReserved);\n" +
            "    [DllImport(\"Wlanapi.dll\")]\n" +
            "    public static extern void WlanFreeMemory(IntPtr pMemory);\n" +
            "    [DllImport(\"Wlanapi.dll\")]\n" +
            "    public static extern uint WlanCloseHandle(IntPtr hClientHandle, IntPtr pReserved);\n" +
            "    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]\n" +
            "    public struct WLAN_INTERFACE_INFO {\n" +
            "        public Guid InterfaceGuid;\n" +
            "        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 256)]\n" +
            "        public string strInterfaceDescription;\n" +
            "        public uint isState;\n" +
            "    }\n" +
            "    public static void Scan() {\n" +
            "        uint negotiatedVersion; IntPtr clientHandle;\n" +
            "        if (WlanOpenHandle(2, IntPtr.Zero, out negotiatedVersion, out clientHandle) != 0) return;\n" +
            "        IntPtr interfaceListPtr;\n" +
            "        if (WlanEnumInterfaces(clientHandle, IntPtr.Zero, out interfaceListPtr) == 0) {\n" +
            "            uint numItems = (uint)Marshal.ReadInt32(interfaceListPtr);\n" +
            "            IntPtr infoPtr = new IntPtr(interfaceListPtr.ToInt64() + 8);\n" +
            "            for (int i = 0; i < numItems; i++) {\n" +
            "                WLAN_INTERFACE_INFO info = (WLAN_INTERFACE_INFO)Marshal.PtrToStructure(infoPtr, typeof(WLAN_INTERFACE_INFO));\n" +
            "                WlanScan(clientHandle, ref info.InterfaceGuid, IntPtr.Zero, IntPtr.Zero, IntPtr.Zero);\n" +
            "                infoPtr = new IntPtr(infoPtr.ToInt64() + Marshal.SizeOf(typeof(WLAN_INTERFACE_INFO)));\n" +
            "            }\n" +
            "            WlanFreeMemory(interfaceListPtr);\n" +
            "        }\n" +
            "        WlanCloseHandle(clientHandle, IntPtr.Zero);\n" +
            "    }\n" +
            "}\n" +
            "\"@\n" +
            "Add-Type -TypeDefinition $code -Language CSharp\n" +
            "[NativeWifiScan]::Scan()\n";

        try {
            // PowerShell -EncodedCommand requires UTF-16LE Base64 — done entirely in memory
            byte[] scriptBytes = script.getBytes("UTF-16LE");
            String encodedScript = java.util.Base64.getEncoder().encodeToString(scriptBytes);
            executeCommandArray(new String[]{
                "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-EncodedCommand", encodedScript
            });
            // Physical hardware scan requires ~3.5s to populate the Windows OS cache
            Thread.sleep(3500);
        } catch (Exception e) {
            System.err.println("[NetworkAdapter] Error forcing WiFi scan: " + e.getMessage());
        }
    }

    /**
     * Scan WiFi networks on Windows using netsh
     */
    private static List<AvailableNetwork> scanWindowsWifi() {
        // Trigger hardware scan FIRST so netsh returns up-to-date results
        forceWindowsWifiScan();

        List<AvailableNetwork> networks = new ArrayList<>();

        try {
            String output = executeCommand("netsh wlan show networks mode=bssid");
            String[] lines = output.split("\n");

            AvailableNetwork currentNetwork = null;

            for (String line : lines) {
                line = line.trim();

                if (line.startsWith("SSID")) {
                    if (currentNetwork != null) {
                        networks.add(currentNetwork);
                    }

                    currentNetwork = new AvailableNetwork();
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        currentNetwork.ssid = parts[1].trim();
                    }
                }

                if (currentNetwork != null) {
                    if (line.startsWith("BSSID")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            currentNetwork.bssid = parts[1].trim();
                        }
                    } else if (line.startsWith("Signal")) {
                        Pattern pattern = Pattern.compile("(\\d+)%");
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            currentNetwork.signalStrength = Integer.parseInt(matcher.group(1));
                        }
                    } else if (line.startsWith("Channel")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            try {
                                currentNetwork.channel = Integer.parseInt(parts[1].trim());
                            } catch (NumberFormatException e) {
                                currentNetwork.channel = 0;
                            }
                        }
                    } else if (line.contains("Authentication")) {
                        currentNetwork.isSecure = !line.contains("Open");
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            currentNetwork.securityType = parts[1].trim();
                        }
                    }
                }
            }

            if (currentNetwork != null) {
                networks.add(currentNetwork);
            }

            // Check which network is currently connected
            String connectedOutput = executeCommand("netsh wlan show interfaces");
            for (String line : connectedOutput.split("\n")) {
                if (line.trim().startsWith("SSID")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        String connectedSSID = parts[1].trim();
                        for (AvailableNetwork network : networks) {
                            if (network.ssid.equals(connectedSSID)) {
                                network.isConnected = true;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error scanning Windows WiFi: " + e.getMessage());
        }

        return networks;
    }

    /**
     * Scan WiFi networks on Linux using iwlist
     */
    private static List<AvailableNetwork> scanLinuxWifi() {
        List<AvailableNetwork> networks = new ArrayList<>();

        try {
            // Try iwlist first
            String output = executeCommand("sudo iwlist wlan0 scan");

            if (output.isEmpty() || output.contains("command not found")) {
                // Try iw as alternative
                output = executeCommand("sudo iw dev wlan0 scan");
            }

            String[] blocks = output.split("Cell \\d+");

            for (String block : blocks) {
                if (block.trim().isEmpty()) continue;

                AvailableNetwork network = new AvailableNetwork();

                // Parse ESSID/SSID
                Pattern ssidPattern = Pattern.compile("ESSID:\"([^\"]+)\"");
                Matcher ssidMatcher = ssidPattern.matcher(block);
                if (ssidMatcher.find()) {
                    network.ssid = ssidMatcher.group(1);
                } else {
                    Pattern ssidPattern2 = Pattern.compile("SSID: (.+)");
                    Matcher ssidMatcher2 = ssidPattern2.matcher(block);
                    if (ssidMatcher2.find()) {
                        network.ssid = ssidMatcher2.group(1).trim();
                    }
                }

                // Parse signal strength
                Pattern signalPattern = Pattern.compile("Quality=(\\d+)/(\\d+)");
                Matcher signalMatcher = signalPattern.matcher(block);
                if (signalMatcher.find()) {
                    int quality = Integer.parseInt(signalMatcher.group(1));
                    int maxQuality = Integer.parseInt(signalMatcher.group(2));
                    network.signalStrength = (quality * 100) / maxQuality;
                }

                // Parse channel
                Pattern channelPattern = Pattern.compile("Channel:(\\d+)");
                Matcher channelMatcher = channelPattern.matcher(block);
                if (channelMatcher.find()) {
                    network.channel = Integer.parseInt(channelMatcher.group(1));
                }

                // Parse security
                network.isSecure = block.contains("Encryption key:on") || block.contains("WPA") || block.contains("WPA2");
                if (block.contains("WPA2")) {
                    network.securityType = "WPA2";
                } else if (block.contains("WPA")) {
                    network.securityType = "WPA";
                } else {
                    network.securityType = "Open";
                }

                if (network.ssid != null && !network.ssid.isEmpty()) {
                    networks.add(network);
                }
            }

            // Check connected network
            String connectedOutput = executeCommand("iwgetid -r");
            if (!connectedOutput.isEmpty()) {
                String connectedSSID = connectedOutput.trim();
                for (AvailableNetwork network : networks) {
                    if (network.ssid.equals(connectedSSID)) {
                        network.isConnected = true;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error scanning Linux WiFi: " + e.getMessage());
        }

        return networks;
    }

    /**
     * Scan WiFi networks on macOS using airport
     */
    private static List<AvailableNetwork> scanMacWifi() {
        List<AvailableNetwork> networks = new ArrayList<>();

        try {
            String output = executeCommand("/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport -s");
            String[] lines = output.split("\n");

            // Skip header line
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length >= 6) {
                    AvailableNetwork network = new AvailableNetwork();

                    network.ssid = parts[0];
                    network.bssid = parts[1];

                    // Signal strength (convert from dBm to percentage)
                    try {
                        int rssi = Integer.parseInt(parts[2]);
                        network.signalStrength = Math.min(100, Math.max(0, (rssi + 100) * 2));
                    } catch (NumberFormatException e) {
                        network.signalStrength = 0;
                    }

                    // Channel
                    try {
                        network.channel = Integer.parseInt(parts[3]);
                    } catch (NumberFormatException e) {
                        network.channel = 0;
                    }

                    // Security
                    String security = parts[parts.length - 1];
                    network.isSecure = !security.equals("NONE") && !security.equals("--");
                    network.securityType = security;

                    networks.add(network);
                }
            }

            // Check connected network
            String connectedOutput = executeCommand("/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport -I");
            for (String line : connectedOutput.split("\n")) {
                if (line.trim().startsWith("SSID:")) {
                    String connectedSSID = line.split(":", 2)[1].trim();
                    for (AvailableNetwork network : networks) {
                        if (network.ssid.equals(connectedSSID)) {
                            network.isConnected = true;
                        }
                    }
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("Error scanning macOS WiFi: " + e.getMessage());
        }

        return networks;
    }

    /**
     * Execute a system command and return output
     */
    private static String executeCommand(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Error executing command: " + e.getMessage());
        }
        return output.toString();
    }

    /**
     * Execute a command with explicit argument array (avoids tokenization issues).
     * Essential for PowerShell commands containing $_ and complex quoting.
     */
    private static String executeCommandArray(String[] command) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Error executing command array: " + e.getMessage());
        }
        return output.toString();
    }

    /**
     * Get latency by pinging an IP address
     */
    public static long getLatencyMs(String ipAddress) {
        try {
            long startTime = System.nanoTime();
            InetAddress address = InetAddress.getByName(ipAddress);
            boolean reachable = address.isReachable(500); // 500ms timeout for fast refresh
            long elapsed = (System.nanoTime() - startTime) / 1_000_000; // nano to ms

            if (reachable) {
                return elapsed;
            } else {
                return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Check if an IP address is reachable
     */
    public static boolean isReachable(String ipAddress, int timeoutMs) {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            return address.isReachable(timeoutMs);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get packet loss percentage
     */
    public static double getPacketLossPercent(String ipAddress, int pingCount) {
        int successful = 0;
        for (int i = 0; i < pingCount; i++) {
            if (isReachable(ipAddress, 300)) { // 300ms timeout for fast refresh
                successful++;
            }
        }
        return ((pingCount - successful) / (double) pingCount) * 100.0;
    }

    /**
     * Get bandwidth usage in Mbps for a specific interface
     */
    public static double getBandwidthMbps(String interfaceName) {
        SystemNetworkStats stats = getAllNetworkStats();
        for (InterfaceStats ifStats : stats.interfaces) {
            if (ifStats.name.equals(interfaceName)) {
                return (ifStats.bytesReceived + ifStats.bytesSent) * 8.0 / 1_000_000.0;
            }
        }
        return 0.0;
    }

    /**
     * Get system-wide network summary
     */
    public static String getNetworkSummary() {
        SystemNetworkStats stats = getAllNetworkStats();
        return String.format(
            "Active Interfaces: %d | Total RX: %.2f MB | Total TX: %.2f MB | Avg Utilization: %.1f%%",
            stats.activeInterfaces,
            stats.totalBytesReceived / 1_000_000.0,
            stats.totalBytesSent / 1_000_000.0,
            stats.averageUtilization
        );
    }

    // Helper methods
    private static long readLongFromFile(String filePath) {
        try {
            BufferedReader reader = new BufferedReader(new java.io.FileReader(filePath));
            String line = reader.readLine();
            reader.close();
            return Long.parseLong(line.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static long parseLong(String str) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static InterfaceStats cloneStats(InterfaceStats stats) {
        InterfaceStats clone = new InterfaceStats();
        clone.name = stats.name;
        clone.bytesReceived = stats.bytesReceived;
        clone.bytesSent = stats.bytesSent;
        clone.packetsReceived = stats.packetsReceived;
        clone.packetsSent = stats.packetsSent;
        clone.speed = stats.speed;
        return clone;
    }

    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        System.out.println("=== NetworkAdapter Test with WiFi Scanning ===\n");

        // Test 1: Network interfaces
        System.out.println("1. Network Interfaces:");
        SystemNetworkStats stats = getAllNetworkStats();
        for (InterfaceStats iface : stats.interfaces) {
            System.out.println("   " + iface);
        }

        // Test 2: WiFi scanning (NEW)
        System.out.println("\n2. Available WiFi Networks:");
        List<AvailableNetwork> networks = scanAvailableNetworks();
        if (networks.isEmpty()) {
            System.out.println("   No networks found (may need elevated privileges)");
        } else {
            for (AvailableNetwork network : networks) {
                System.out.println("   " + network);
            }
        }

        // Test 3: Connectivity
        System.out.println("\n3. Connectivity Test:");
        String testIP = "8.8.8.8";
        long latency = getLatencyMs(testIP);
        System.out.println("   " + testIP + ": " + (latency >= 0 ? latency + " ms" : "Unreachable"));
    }
}
