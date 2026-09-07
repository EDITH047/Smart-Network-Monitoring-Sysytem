package com.networkmonitor.dao;

import com.networkmonitor.config.DatabaseConfig;
import com.networkmonitor.model.SecurityEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SecurityEventDAO - Data Access Object for SecurityEvent table
 * Handles all JDBC operations related to security events/threats
 */
public class SecurityEventDAO {

    /**
     * Insert a new security event
     */
    public boolean insertEvent(SecurityEvent event) {
        String sql = "INSERT INTO security_events (device_id, event_type, severity, source_ip, description, is_resolved, detected_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, event.getDeviceId());
            ps.setString(2, event.getEventType());
            ps.setString(3, event.getSeverity());
            ps.setString(4, event.getSourceIp());
            ps.setString(5, event.getDescription());
            ps.setBoolean(6, event.isResolved());

            int rowsInserted = ps.executeUpdate();
            System.out.println("[SecurityEventDAO] Event inserted: " + event.getEventType());
            return rowsInserted > 0;

        } catch (SQLException e) {
            System.err.println("[SecurityEventDAO] Error inserting event: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all unresolved events
     */
    public List<SecurityEvent> getUnresolvedEvents() {
        List<SecurityEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM security_events WHERE is_resolved = FALSE ORDER BY detected_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("[SecurityEventDAO] Error getting unresolved events: " + e.getMessage());
        }

        return events;
    }

    /**
     * Get events by device
     */
    public List<SecurityEvent> getEventsByDevice(int deviceId) {
        List<SecurityEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM security_events WHERE device_id = ? ORDER BY detected_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, deviceId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("[SecurityEventDAO] Error getting events by device: " + e.getMessage());
        }

        return events;
    }

    /**
     * Get events by type
     */
    public List<SecurityEvent> getEventsByType(String eventType) {
        List<SecurityEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM security_events WHERE event_type = ? ORDER BY detected_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, eventType);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("[SecurityEventDAO] Error getting events by type: " + e.getMessage());
        }

        return events;
    }

    /**
     * Get events by severity
     */
    public List<SecurityEvent> getEventsBySeverity(String severity) {
        List<SecurityEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM security_events WHERE severity = ? ORDER BY detected_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, severity);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("[SecurityEventDAO] Error getting events by severity: " + e.getMessage());
        }

        return events;
    }

    /**
     * Get critical events
     */
    public List<SecurityEvent> getCriticalEvents() {
        return getEventsBySeverity("CRITICAL");
    }

    /**
     * Get events by date range
     */
    public List<SecurityEvent> getEventsByDateRange(Timestamp from, Timestamp to) {
        List<SecurityEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM security_events WHERE detected_at BETWEEN ? AND ? ORDER BY detected_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, from);
            ps.setTimestamp(2, to);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("[SecurityEventDAO] Error getting events by date range: " + e.getMessage());
        }

        return events;
    }

    /**
     * Resolve an event
     */
    public boolean resolveEvent(int eventId) {
        String sql = "UPDATE security_events SET is_resolved = TRUE, resolved_at = NOW() WHERE event_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, eventId);
            int rowsUpdated = ps.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("[SecurityEventDAO] Error resolving event: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get event by ID
     */
    public SecurityEvent getEventById(int eventId) {
        String sql = "SELECT * FROM security_events WHERE event_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, eventId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToEvent(rs);
            }

        } catch (SQLException e) {
            System.err.println("[SecurityEventDAO] Error getting event by ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get event count by type
     */
    public int getEventCountByType(String eventType) {
        String sql = "SELECT COUNT(*) as count FROM security_events WHERE event_type = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, eventType);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("[SecurityEventDAO] Error getting event count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get events from a source IP
     */
    public List<SecurityEvent> getEventsBySourceIp(String sourceIp) {
        List<SecurityEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM security_events WHERE source_ip = ? ORDER BY detected_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sourceIp);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("[SecurityEventDAO] Error getting events by source IP: " + e.getMessage());
        }

        return events;
    }

    /**
     * Helper method to map ResultSet to SecurityEvent
     */
    private SecurityEvent mapResultSetToEvent(ResultSet rs) throws SQLException {
        SecurityEvent event = new SecurityEvent();
        event.setEventId(rs.getInt("event_id"));
        event.setDeviceId(rs.getInt("device_id"));
        event.setEventType(rs.getString("event_type"));
        event.setSeverity(rs.getString("severity"));
        event.setSourceIp(rs.getString("source_ip"));
        event.setDescription(rs.getString("description"));
        event.setResolved(rs.getBoolean("is_resolved"));
        event.setDetectedAt(rs.getTimestamp("detected_at"));
        event.setResolvedAt(rs.getTimestamp("resolved_at"));
        return event;
    }
}
