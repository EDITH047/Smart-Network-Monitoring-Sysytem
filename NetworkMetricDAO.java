package com.networkmonitor.dao;

import com.networkmonitor.config.DatabaseConfig;
import com.networkmonitor.model.NetworkMetric;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * NetworkMetricDAO - Data Access Object for NetworkMetric table
 * Handles all JDBC operations related to network performance metrics
 */
public class NetworkMetricDAO {

    /**
     * Insert a new metric record
     */
    public boolean insertMetric(NetworkMetric metric) {
        String sql = "INSERT INTO network_metrics (device_id, bandwidth_usage, latency_ms, packet_loss_pct, packets_in, packets_out, recorded_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, metric.getDeviceId());
            ps.setDouble(2, metric.getBandwidthUsage());
            ps.setDouble(3, metric.getLatencyMs());
            ps.setDouble(4, metric.getPacketLossPct());
            ps.setLong(5, metric.getPacketsIn());
            ps.setLong(6, metric.getPacketsOut());

            int rowsInserted = ps.executeUpdate();
            return rowsInserted > 0;

        } catch (SQLException e) {
            System.err.println("[NetworkMetricDAO] Error inserting metric: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get latest metric for a specific device
     */
    public NetworkMetric getLatestByDevice(int deviceId) {
        String sql = "SELECT * FROM network_metrics WHERE device_id = ? ORDER BY recorded_at DESC LIMIT 1";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, deviceId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToMetric(rs);
            }

        } catch (SQLException e) {
            System.err.println("[NetworkMetricDAO] Error getting latest metric: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get all latest metrics (one per device)
     */
    public List<NetworkMetric> getLatestMetrics() {
        List<NetworkMetric> metrics = new ArrayList<>();
        String sql = "SELECT m.* FROM network_metrics m " +
                "INNER JOIN (SELECT device_id, MAX(recorded_at) as max_time FROM network_metrics GROUP BY device_id) latest " +
                "ON m.device_id = latest.device_id AND m.recorded_at = latest.max_time";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                metrics.add(mapResultSetToMetric(rs));
            }

        } catch (SQLException e) {
            System.err.println("[NetworkMetricDAO] Error getting latest metrics: " + e.getMessage());
        }

        return metrics;
    }

    /**
     * Get metrics for a device within a date range
     */
    public List<NetworkMetric> getMetricsByDeviceAndDateRange(int deviceId, Timestamp from, Timestamp to) {
        List<NetworkMetric> metrics = new ArrayList<>();
        String sql = "SELECT * FROM network_metrics WHERE device_id = ? AND recorded_at BETWEEN ? AND ? " +
                "ORDER BY recorded_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, deviceId);
            ps.setTimestamp(2, from);
            ps.setTimestamp(3, to);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                metrics.add(mapResultSetToMetric(rs));
            }

        } catch (SQLException e) {
            System.err.println("[NetworkMetricDAO] Error getting metrics by date range: " + e.getMessage());
        }

        return metrics;
    }

    /**
     * Get average metrics for a device over last N hours
     */
    public NetworkMetric getAverageMetrics(int deviceId, int hours) {
        String sql = "SELECT device_id, " +
                "AVG(bandwidth_usage) as bandwidth_usage, " +
                "AVG(latency_ms) as latency_ms, " +
                "AVG(packet_loss_pct) as packet_loss_pct, " +
                "SUM(packets_in) as packets_in, " +
                "SUM(packets_out) as packets_out, " +
                "MAX(recorded_at) as recorded_at " +
                "FROM network_metrics " +
                "WHERE device_id = ? AND recorded_at >= DATE_SUB(NOW(), INTERVAL ? HOUR) " +
                "GROUP BY device_id";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, deviceId);
            ps.setInt(2, hours);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                NetworkMetric metric = new NetworkMetric();
                metric.setDeviceId(rs.getInt("device_id"));
                metric.setBandwidthUsage(rs.getDouble("bandwidth_usage"));
                metric.setLatencyMs(rs.getDouble("latency_ms"));
                metric.setPacketLossPct(rs.getDouble("packet_loss_pct"));
                metric.setPacketsIn(rs.getLong("packets_in"));
                metric.setPacketsOut(rs.getLong("packets_out"));
                metric.setRecordedAt(rs.getTimestamp("recorded_at"));
                return metric;
            }

        } catch (SQLException e) {
            System.err.println("[NetworkMetricDAO] Error getting average metrics: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get peak bandwidth for a device in last N hours
     */
    public double getPeakBandwidth(int deviceId, int hours) {
        String sql = "SELECT MAX(bandwidth_usage) as peak FROM network_metrics " +
                "WHERE device_id = ? AND recorded_at >= DATE_SUB(NOW(), INTERVAL ? HOUR)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, deviceId);
            ps.setInt(2, hours);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("peak");
            }

        } catch (SQLException e) {
            System.err.println("[NetworkMetricDAO] Error getting peak bandwidth: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get high latency events (latency > threshold) for a device
     */
    public List<NetworkMetric> getHighLatencyEvents(int deviceId, double latencyThreshold, int hours) {
        List<NetworkMetric> metrics = new ArrayList<>();
        String sql = "SELECT * FROM network_metrics " +
                "WHERE device_id = ? AND latency_ms > ? AND recorded_at >= DATE_SUB(NOW(), INTERVAL ? HOUR) " +
                "ORDER BY recorded_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, deviceId);
            ps.setDouble(2, latencyThreshold);
            ps.setInt(3, hours);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                metrics.add(mapResultSetToMetric(rs));
            }

        } catch (SQLException e) {
            System.err.println("[NetworkMetricDAO] Error getting high latency events: " + e.getMessage());
        }

        return metrics;
    }

    /**
     * Get packet loss events (loss > threshold) for a device
     */
    public List<NetworkMetric> getHighPacketLossEvents(int deviceId, double lossThreshold, int hours) {
        List<NetworkMetric> metrics = new ArrayList<>();
        String sql = "SELECT * FROM network_metrics " +
                "WHERE device_id = ? AND packet_loss_pct > ? AND recorded_at >= DATE_SUB(NOW(), INTERVAL ? HOUR) " +
                "ORDER BY recorded_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, deviceId);
            ps.setDouble(2, lossThreshold);
            ps.setInt(3, hours);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                metrics.add(mapResultSetToMetric(rs));
            }

        } catch (SQLException e) {
            System.err.println("[NetworkMetricDAO] Error getting high packet loss events: " + e.getMessage());
        }

        return metrics;
    }

    /**
     * Delete old metrics (older than N days) for cleanup
     */
    public int deleteOldMetrics(int days) {
        String sql = "DELETE FROM network_metrics WHERE recorded_at < DATE_SUB(NOW(), INTERVAL ? DAY)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, days);
            int rowsDeleted = ps.executeUpdate();
            System.out.println("[NetworkMetricDAO] Deleted " + rowsDeleted + " old metrics");
            return rowsDeleted;

        } catch (SQLException e) {
            System.err.println("[NetworkMetricDAO] Error deleting old metrics: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get total metrics count
     */
    public int getMetricsCount() {
        String sql = "SELECT COUNT(*) as count FROM network_metrics";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("[NetworkMetricDAO] Error getting metrics count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Helper method to map ResultSet to NetworkMetric
     */
    private NetworkMetric mapResultSetToMetric(ResultSet rs) throws SQLException {
        NetworkMetric metric = new NetworkMetric();
        metric.setMetricId(rs.getInt("metric_id"));
        metric.setDeviceId(rs.getInt("device_id"));
        metric.setBandwidthUsage(rs.getDouble("bandwidth_usage"));
        metric.setLatencyMs(rs.getDouble("latency_ms"));
        metric.setPacketLossPct(rs.getDouble("packet_loss_pct"));
        metric.setPacketsIn(rs.getLong("packets_in"));
        metric.setPacketsOut(rs.getLong("packets_out"));
        metric.setRecordedAt(rs.getTimestamp("recorded_at"));
        return metric;
    }
}
