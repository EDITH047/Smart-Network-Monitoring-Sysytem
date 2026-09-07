package com.networkmonitor.dao;

import com.networkmonitor.config.DatabaseConfig;
import com.networkmonitor.model.Device;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DeviceDAO - Data Access Object for Device table
 * Handles all JDBC operations related to network devices
 */
public class DeviceDAO {

    /**
     * Add a new device
     */
    public boolean addDevice(Device device) {
        String sql = "INSERT INTO devices (device_name, ip_address, mac_address, device_type, location, status, added_by, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, device.getDeviceName());
            ps.setString(2, device.getIpAddress());
            ps.setString(3, device.getMacAddress());
            ps.setString(4, device.getDeviceType());
            ps.setString(5, device.getLocation());
            ps.setString(6, device.getStatus());
            ps.setInt(7, device.getAddedBy());

            int rowsInserted = ps.executeUpdate();
            System.out.println("[DeviceDAO] Device added: " + device.getDeviceName());
            return rowsInserted > 0;

        } catch (SQLException e) {
            System.err.println("[DeviceDAO] Error adding device: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all devices
     */
    public List<Device> getAllDevices() {
        List<Device> devices = new ArrayList<>();
        String sql = "SELECT * FROM devices ORDER BY device_name";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                devices.add(mapResultSetToDevice(rs));
            }

        } catch (SQLException e) {
            System.err.println("[DeviceDAO] Error retrieving all devices: " + e.getMessage());
        }

        return devices;
    }

    /**
     * Get device by ID
     */
    public Device getDeviceById(int deviceId) {
        String sql = "SELECT * FROM devices WHERE device_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, deviceId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToDevice(rs);
            }

        } catch (SQLException e) {
            System.err.println("[DeviceDAO] Error getting device by ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get devices by status
     */
    public List<Device> getDevicesByStatus(String status) {
        List<Device> devices = new ArrayList<>();
        String sql = "SELECT * FROM devices WHERE status = ? ORDER BY device_name";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                devices.add(mapResultSetToDevice(rs));
            }

        } catch (SQLException e) {
            System.err.println("[DeviceDAO] Error getting devices by status: " + e.getMessage());
        }

        return devices;
    }

    /**
     * Get devices by type
     */
    public List<Device> getDevicesByType(String deviceType) {
        List<Device> devices = new ArrayList<>();
        String sql = "SELECT * FROM devices WHERE device_type = ? ORDER BY device_name";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, deviceType);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                devices.add(mapResultSetToDevice(rs));
            }

        } catch (SQLException e) {
            System.err.println("[DeviceDAO] Error getting devices by type: " + e.getMessage());
        }

        return devices;
    }

    /**
     * Get device by IP address
     */
    public Device getDeviceByIp(String ipAddress) {
        String sql = "SELECT * FROM devices WHERE ip_address = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ipAddress);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToDevice(rs);
            }

        } catch (SQLException e) {
            System.err.println("[DeviceDAO] Error getting device by IP: " + e.getMessage());
        }

        return null;
    }

    /**
     * Update device information
     */
    public boolean updateDevice(Device device) {
        String sql = "UPDATE devices SET device_name = ?, mac_address = ?, device_type = ?, location = ?, status = ? WHERE device_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, device.getDeviceName());
            ps.setString(2, device.getMacAddress());
            ps.setString(3, device.getDeviceType());
            ps.setString(4, device.getLocation());
            ps.setString(5, device.getStatus());
            ps.setInt(6, device.getDeviceId());

            int rowsUpdated = ps.executeUpdate();
            System.out.println("[DeviceDAO] Device updated: " + device.getDeviceName());
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("[DeviceDAO] Error updating device: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update device status
     */
    public boolean updateDeviceStatus(int deviceId, String status) {
        String sql = "UPDATE devices SET status = ? WHERE device_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, deviceId);

            int rowsUpdated = ps.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("[DeviceDAO] Error updating device status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete a device
     */
    public boolean deleteDevice(int deviceId) {
        String sql = "DELETE FROM devices WHERE device_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, deviceId);
            int rowsDeleted = ps.executeUpdate();
            System.out.println("[DeviceDAO] Device deleted: " + deviceId);
            return rowsDeleted > 0;

        } catch (SQLException e) {
            System.err.println("[DeviceDAO] Error deleting device: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get device count
     */
    public int getDeviceCount() {
        String sql = "SELECT COUNT(*) as count FROM devices";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("[DeviceDAO] Error getting device count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get count of online devices
     */
    public int getOnlineDeviceCount() {
        String sql = "SELECT COUNT(*) as count FROM devices WHERE status = 'ONLINE'";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("[DeviceDAO] Error getting online device count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Check if device exists by IP
     */
    public boolean deviceExistsByIp(String ipAddress) {
        return getDeviceByIp(ipAddress) != null;
    }

    /**
     * Helper method to map ResultSet to Device
     */
    private Device mapResultSetToDevice(ResultSet rs) throws SQLException {
        Device device = new Device();
        device.setDeviceId(rs.getInt("device_id"));
        device.setDeviceName(rs.getString("device_name"));
        device.setIpAddress(rs.getString("ip_address"));
        device.setMacAddress(rs.getString("mac_address"));
        device.setDeviceType(rs.getString("device_type"));
        device.setLocation(rs.getString("location"));
        device.setStatus(rs.getString("status"));
        device.setAddedBy(rs.getInt("added_by"));
        device.setCreatedAt(rs.getTimestamp("created_at"));
        return device;
    }
}
