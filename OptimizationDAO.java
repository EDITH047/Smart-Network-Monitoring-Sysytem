package com.networkmonitor.dao;

import com.networkmonitor.config.DatabaseConfig;
import com.networkmonitor.model.OptimizationResult;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * OptimizationDAO - Data Access Object for OptimizationResult table
 * Handles all JDBC operations related to bandwidth optimization analysis
 */
public class OptimizationDAO {

    /**
     * Insert optimization result
     */
    public boolean insertResult(OptimizationResult result) {
        String sql = "INSERT INTO optimization_results (device_id, current_bandwidth, recommended_bandwidth, optimization_score, suggestion, analyzed_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, result.getDeviceId());
            ps.setDouble(2, result.getCurrentBandwidth());
            ps.setDouble(3, result.getRecommendedBandwidth());
            ps.setInt(4, result.getOptimizationScore());
            ps.setString(5, result.getSuggestion());

            int rowsInserted = ps.executeUpdate();
            System.out.println("[OptimizationDAO] Result inserted for device: " + result.getDeviceId());
            return rowsInserted > 0;

        } catch (SQLException e) {
            System.err.println("[OptimizationDAO] Error inserting result: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get latest result for a device
     */
    public OptimizationResult getLatestResultByDevice(int deviceId) {
        String sql = "SELECT * FROM optimization_results WHERE device_id = ? ORDER BY analyzed_at DESC LIMIT 1";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, deviceId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToResult(rs);
            }

        } catch (SQLException e) {
            System.err.println("[OptimizationDAO] Error getting latest result: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get all latest results (one per device)
     */
    public List<OptimizationResult> getLatestResults() {
        List<OptimizationResult> results = new ArrayList<>();
        String sql = "SELECT r.* FROM optimization_results r " +
                "INNER JOIN (SELECT device_id, MAX(analyzed_at) as max_time FROM optimization_results GROUP BY device_id) latest " +
                "ON r.device_id = latest.device_id AND r.analyzed_at = latest.max_time";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                results.add(mapResultSetToResult(rs));
            }

        } catch (SQLException e) {
            System.err.println("[OptimizationDAO] Error getting latest results: " + e.getMessage());
        }

        return results;
    }

    /**
     * Get results by score range
     */
    public List<OptimizationResult> getResultsByScoreRange(int minScore, int maxScore) {
        List<OptimizationResult> results = new ArrayList<>();
        String sql = "SELECT * FROM optimization_results WHERE optimization_score BETWEEN ? AND ? ORDER BY optimization_score DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, minScore);
            ps.setInt(2, maxScore);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                results.add(mapResultSetToResult(rs));
            }

        } catch (SQLException e) {
            System.err.println("[OptimizationDAO] Error getting results by score range: " + e.getMessage());
        }

        return results;
    }

    /**
     * Get low score results (optimization needed)
     */
    public List<OptimizationResult> getLowScoreResults(int threshold) {
        return getResultsByScoreRange(0, threshold);
    }

    /**
     * Get high score results (well optimized)
     */
    public List<OptimizationResult> getHighScoreResults(int threshold) {
        return getResultsByScoreRange(threshold, 100);
    }

    /**
     * Get average optimization score
     */
    public double getAverageScore() {
        String sql = "SELECT AVG(optimization_score) as avg_score FROM optimization_results";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble("avg_score");
            }

        } catch (SQLException e) {
            System.err.println("[OptimizationDAO] Error getting average score: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get results by date range
     */
    public List<OptimizationResult> getResultsByDateRange(Timestamp from, Timestamp to) {
        List<OptimizationResult> results = new ArrayList<>();
        String sql = "SELECT * FROM optimization_results WHERE analyzed_at BETWEEN ? AND ? ORDER BY analyzed_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, from);
            ps.setTimestamp(2, to);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                results.add(mapResultSetToResult(rs));
            }

        } catch (SQLException e) {
            System.err.println("[OptimizationDAO] Error getting results by date range: " + e.getMessage());
        }

        return results;
    }

    /**
     * Helper method to map ResultSet to OptimizationResult
     */
    private OptimizationResult mapResultSetToResult(ResultSet rs) throws SQLException {
        OptimizationResult result = new OptimizationResult();
        result.setResultId(rs.getInt("result_id"));
        result.setDeviceId(rs.getInt("device_id"));
        result.setCurrentBandwidth(rs.getDouble("current_bandwidth"));
        result.setRecommendedBandwidth(rs.getDouble("recommended_bandwidth"));
        result.setOptimizationScore(rs.getInt("optimization_score"));
        result.setSuggestion(rs.getString("suggestion"));
        result.setAnalyzedAt(rs.getTimestamp("analyzed_at"));
        return result;
    }
}
