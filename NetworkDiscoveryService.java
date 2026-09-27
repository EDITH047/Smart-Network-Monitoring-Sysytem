package com.networkmonitor.service;

import com.networkmonitor.dao.DeviceDAO;
import com.networkmonitor.model.Device;
import com.networkmonitor.util.NetworkAdapter;
import com.networkmonitor.util.NetworkAdapter.SystemNetworkStats;
import com.networkmonitor.util.NetworkAdapter.InterfaceStats;

import java.util.List;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.util.regex.*;

/**
 * NetworkDiscoveryService — Auto-detects the current system's network
 * interfaces and default gateway on startup.
 *
 * Ensures the devices table contains entries for THIS machine,
 * regardless of which system the app is running on.
 */
public class NetworkDiscoveryService {

    private final DeviceDAO deviceDAO = new DeviceDAO();

    /**
     * Run full auto-discovery. Call this once during application startup,
     * AFTER the database is ready.
     */
    public void discoverAndRegister() {
        System.out.println("[NetworkDiscovery] Starting auto-discovery...");
        
        // 0. Remove stale/hardcoded devices that were not auto-discovered
        cleanStaleDevices();
        
        // 1. Discover and register local machine's interfaces
        discoverLocalInterfaces();
        
        // 2. Discover and register default gateway (router)
        discoverDefaultGateway();
        
        // 3. Refresh device cache
        DeviceCache.getInstance().invalidate();
        
        System.out.println("[NetworkDiscovery] Auto-discovery complete.");
    }

    /**
     * Remove devices from the database that were NOT auto-discovered.
     * This ensures only real, currently-present network devices appear
     * in the device list and monitoring page.
     */
    private void cleanStaleDevices() {
        try {
            List<Device> allDevices = deviceDAO.getAllDevices();
            int removed = 0;
            for (Device d : allDevices) {
                String desc = d.getDescription();
                // Keep only auto-discovered devices
                if (desc == null || !desc.contains("Auto-discovered")) {
                    deviceDAO.deleteDevice(d.getDeviceId());
                    removed++;
                    System.out.println("[NetworkDiscovery] Removed stale device: " + d.getDeviceName() + " (" + d.getIpAddress() + ")");
                }
            }
            if (removed > 0) {
                System.out.println("[NetworkDiscovery] Cleaned " + removed + " stale/hardcoded device(s).");
            }
        } catch (Exception e) {
            System.err.println("[NetworkDiscovery] Error cleaning stale devices: " + e.getMessage());
        }
    }

    private void discoverLocalInterfaces() {
        SystemNetworkStats stats = NetworkAdapter.getAllNetworkStats();
        
        for (InterfaceStats iface : stats.interfaces) {
            if (iface.ipAddress == null || "N/A".equals(iface.ipAddress)) continue;
            if (iface.ipAddress.startsWith("169.254")) continue; // Skip APIPA
            
            // Check if a device with this IP already exists
            if (!deviceDAO.deviceExistsByIp(iface.ipAddress)) {
                Device device = new Device();
                device.setDeviceName("Local - " + iface.displayName);
                device.setIpAddress(iface.ipAddress);
                device.setMacAddress(iface.macAddress);
                device.setDeviceType("pc");
                device.setLocation("Local Machine");
                device.setStatus("ONLINE");
                device.setNetworkInterface(iface.name);
                device.setDescription("Auto-discovered local interface");
                device.setAddedBy(1); // system/admin
                device.setLastSeen(new java.sql.Timestamp(System.currentTimeMillis()));
                
                deviceDAO.addDevice(device);
                System.out.println("[NetworkDiscovery] Registered local interface: " 
                    + iface.displayName + " (" + iface.ipAddress + ")");
            }
        }
    }

    private void discoverDefaultGateway() {
        try {
            String gatewayIp = null;
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                // Parse "ipconfig" for Default Gateway
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "ipconfig");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
                String line;
                Pattern gwPattern = Pattern.compile(
                    "Default Gateway[\\s.]*:\\s*(\\d+\\.\\d+\\.\\d+\\.\\d+)");
                while ((line = reader.readLine()) != null) {
                    Matcher m = gwPattern.matcher(line);
                    if (m.find()) {
                        gatewayIp = m.group(1);
                        break;
                    }
                }
                p.waitFor();
            } else {
                // Linux/macOS: parse "ip route" or "route -n get default"
                ProcessBuilder pb = new ProcessBuilder("ip", "route");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("default")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) {
                            gatewayIp = parts[2];
                        }
                        break;
                    }
                }
                p.waitFor();
            }
            
            if (gatewayIp != null && !deviceDAO.deviceExistsByIp(gatewayIp)) {
                Device gateway = new Device();
                gateway.setDeviceName("Default Gateway");
                gateway.setIpAddress(gatewayIp);
                gateway.setDeviceType("router");
                gateway.setLocation("Network Gateway");
                gateway.setStatus("ONLINE");
                gateway.setDescription("Auto-discovered default gateway");
                gateway.setAddedBy(1);
                gateway.setLastSeen(new java.sql.Timestamp(System.currentTimeMillis()));
                
                deviceDAO.addDevice(gateway);
                System.out.println("[NetworkDiscovery] Registered gateway: " + gatewayIp);
            }
        } catch (Exception e) {
            System.err.println("[NetworkDiscovery] Gateway discovery error: " + e.getMessage());
        }
    }
}
