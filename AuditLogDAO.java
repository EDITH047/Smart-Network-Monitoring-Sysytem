package com.networkmonitor.dao;

import com.networkmonitor.config.DatabaseConfig;
import com.networkmonitor.model.AuditLog;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AuditLogDAO - Data Access Object for AuditLog table
 * Handles all JDBC operations related to audit logging
 */
public class AuditLogDAO {

    /**
     * Insert a new audit log entry
     */
    public boolean insertLog(AuditLog log) {
        String sql = "INSERT INTO audit_log (user_id, action, details, ip_address, performed_at) " +
                "VALUES (?, ?, ?, ?, NOW())";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, log.getUserId());
            ps.setString(2, log.getAction());
            ps.setString(3, log.getDetails());
            ps.setString(4, log.getIpAddress());

            int rowsInserted = ps.executeUpdate();
            return rowsInserted > 0;

        } catch (SQLException e) {
            System.err.println("[AuditLogDAO] Error inserting log: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all audit logs
     */
    public List<AuditLog> getAllLogs() {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log ORDER BY performed_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }

        } catch (SQLException e) {
            System.err.println("[AuditLogDAO] Error retrieving all logs: " + e.getMessage());
        }

        return logs;
    }

    /**
     * Get logs by user
     */
    public List<AuditLog> getLogsByUser(int userId) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log WHERE user_id = ? ORDER BY performed_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }

        } catch (SQLException e) {
            System.err.println("[AuditLogDAO] Error getting logs by user: " + e.getMessage());
        }

        return logs;
    }

    /**
     * Get logs by action type
     */
    public List<AuditLog> getLogsByAction(String action) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log WHERE action = ? ORDER BY performed_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, action);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }

        } catch (SQLException e) {
            System.err.println("[AuditLogDAO] Error getting logs by action: " + e.getMessage());
        }

        return logs;
    }

    /**
     * Get logs by date range
     */
    public List<AuditLog> getLogsByDateRange(Timestamp from, Timestamp to) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log WHERE performed_at BETWEEN ? AND ? ORDER BY performed_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, from);
            ps.setTimestamp(2, to);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }

        } catch (SQLException e) {
            System.err.println("[AuditLogDAO] Error getting logs by date range: " + e.getMessage());
        }

        return logs;
    }

    /**
     * Get logs by IP address
     */
    public List<AuditLog> getLogsByIPAddress(String ipAddress) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log WHERE ip_address = ? ORDER BY performed_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ipAddress);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }

        } catch (SQLException e) {
            System.err.println("[AuditLogDAO] Error getting logs by IP: " + e.getMessage());
        }

        return logs;
    }

    /**
     * Get recent logs
     */
    public List<AuditLog> getRecentLogs(int limit) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log ORDER BY performed_at DESC LIMIT ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }

        } catch (SQLException e) {
            System.err.println("[AuditLogDAO] Error getting recent logs: " + e.getMessage());
        }

        return logs;
    }

    /**
     * Get log by ID
     */
    public AuditLog getLogById(int logId) {
        String sql = "SELECT * FROM audit_log WHERE log_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, logId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToLog(rs);
            }

        } catch (SQLException e) {
            System.err.println("[AuditLogDAO] Error getting log by ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get total log count
     */
    public int getLogCount() {
        String sql = "SELECT COUNT(*) as count FROM audit_log";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("[AuditLogDAO] Error getting log count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Delete old logs (older than N days)
     */
    public int deleteOldLogs(int days) {
        String sql = "DELETE FROM audit_log WHERE performed_at < DATE_SUB(NOW(), INTERVAL ? DAY)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, days);
            int rowsDeleted = ps.executeUpdate();
            System.out.println("[AuditLogDAO] Deleted " + rowsDeleted + " old logs");
            return rowsDeleted;

        } catch (SQLException e) {
            System.err.println("[AuditLogDAO] Error deleting old logs: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get logs by user and action
     */
    public List<AuditLog> getLogsByUserAndAction(int userId, String action) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log WHERE user_id = ? AND action = ? ORDER BY performed_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, action);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }

        } catch (SQLException e) {
            System.err.println("[AuditLogDAO] Error getting logs by user and action: " + e.getMessage());
        }

        return logs;
    }

    /**
     * Helper method to map ResultSet to AuditLog
     */
    private AuditLog mapResultSetToLog(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setLogId(rs.getInt("log_id"));
        log.setUserId(rs.getInt("user_id"));
        log.setAction(rs.getString("action"));
        log.setDetails(rs.getString("details"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setPerformedAt(rs.getTimestamp("performed_at"));
        return log;
    }
}
