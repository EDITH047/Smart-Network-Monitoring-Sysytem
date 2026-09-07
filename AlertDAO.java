package com.networkmonitor.dao;

import com.networkmonitor.config.DatabaseConfig;
import com.networkmonitor.model.Alert;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AlertDAO - Data Access Object for Alert table
 * Handles all JDBC operations related to alerts and notifications
 */
public class AlertDAO {

    /**
     * Insert a new alert
     */
    public boolean insertAlert(Alert alert) {
        String sql = "INSERT INTO alerts (device_id, alert_type, severity, message, is_acknowledged, created_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, alert.getDeviceId());
            ps.setString(2, alert.getAlertType());
            ps.setString(3, alert.getSeverity());
            ps.setString(4, alert.getMessage());
            ps.setBoolean(5, alert.isAcknowledged());

            int rowsInserted = ps.executeUpdate();
            return rowsInserted > 0;

        } catch (SQLException e) {
            System.err.println("[AlertDAO] Error inserting alert: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all unacknowledged alerts
     */
    public List<Alert> getUnacknowledgedAlerts() {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM alerts WHERE is_acknowledged = FALSE ORDER BY created_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                alerts.add(mapResultSetToAlert(rs));
            }

        } catch (SQLException e) {
            System.err.println("[AlertDAO] Error getting unacknowledged alerts: " + e.getMessage());
        }

        return alerts;
    }

    /**
     * Get unacknowledged alert count
     */
    public int getUnacknowledgedCount() {
        String sql = "SELECT COUNT(*) as count FROM alerts WHERE is_acknowledged = FALSE";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("[AlertDAO] Error getting unacknowledged count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get all alerts for a device
     */
    public List<Alert> getAlertsByDevice(int deviceId) {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM alerts WHERE device_id = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, deviceId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                alerts.add(mapResultSetToAlert(rs));
            }

        } catch (SQLException e) {
            System.err.println("[AlertDAO] Error getting alerts by device: " + e.getMessage());
        }

        return alerts;
    }

    /**
     * Get alerts by severity
     */
    public List<Alert> getAlertsBySeverity(String severity) {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM alerts WHERE severity = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, severity);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                alerts.add(mapResultSetToAlert(rs));
            }

        } catch (SQLException e) {
            System.err.println("[AlertDAO] Error getting alerts by severity: " + e.getMessage());
        }

        return alerts;
    }

    /**
     * Get critical alerts
     */
    public List<Alert> getCriticalAlerts() {
        return getAlertsBySeverity("CRITICAL");
    }

    /**
     * Acknowledge an alert
     */
    public boolean acknowledgeAlert(int alertId, int userId) {
        String sql = "UPDATE alerts SET is_acknowledged = TRUE, acknowledged_by = ? WHERE alert_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, alertId);

            int rowsUpdated = ps.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("[AlertDAO] Error acknowledging alert: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get alert by ID
     */
    public Alert getAlertById(int alertId) {
        String sql = "SELECT * FROM alerts WHERE alert_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, alertId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToAlert(rs);
            }

        } catch (SQLException e) {
            System.err.println("[AlertDAO] Error getting alert by ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Delete an alert
     */
    public boolean deleteAlert(int alertId) {
        String sql = "DELETE FROM alerts WHERE alert_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, alertId);
            int rowsDeleted = ps.executeUpdate();
            return rowsDeleted > 0;

        } catch (SQLException e) {
            System.err.println("[AlertDAO] Error deleting alert: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get recent alerts
     */
    public List<Alert> getRecentAlerts(int limit) {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM alerts ORDER BY created_at DESC LIMIT ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                alerts.add(mapResultSetToAlert(rs));
            }

        } catch (SQLException e) {
            System.err.println("[AlertDAO] Error getting recent alerts: " + e.getMessage());
        }

        return alerts;
    }

    /**
     * Helper method to map ResultSet to Alert
     */
    private Alert mapResultSetToAlert(ResultSet rs) throws SQLException {
        Alert alert = new Alert();
        alert.setAlertId(rs.getInt("alert_id"));
        alert.setDeviceId(rs.getInt("device_id"));
        alert.setAlertType(rs.getString("alert_type"));
        alert.setSeverity(rs.getString("severity"));
        alert.setMessage(rs.getString("message"));
        alert.setAcknowledged(rs.getBoolean("is_acknowledged"));
        alert.setCreatedAt(rs.getTimestamp("created_at"));
        alert.setAcknowledgedBy(rs.getInt("acknowledged_by"));
        return alert;
    }
}
