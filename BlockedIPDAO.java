package com.networkmonitor.dao;

import com.networkmonitor.config.DatabaseConfig;
import com.networkmonitor.model.BlockedIP;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BlockedIPDAO - Data Access Object for BlockedIP table
 * Handles all JDBC operations related to blocked/blacklisted IPs
 */
public class BlockedIPDAO {

    /**
     * Block an IP address
     */
    public boolean blockIP(String ipAddress, String reason, int blockedBy, boolean isPermanent) {
        String sql = "INSERT INTO blocked_ips (ip_address, reason, blocked_by, blocked_at, is_permanent) " +
                "VALUES (?, ?, ?, NOW(), ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ipAddress);
            ps.setString(2, reason);
            ps.setInt(3, blockedBy);
            ps.setBoolean(4, isPermanent);

            int rowsInserted = ps.executeUpdate();
            System.out.println("[BlockedIPDAO] IP blocked: " + ipAddress);
            return rowsInserted > 0;

        } catch (SQLException e) {
            System.err.println("[BlockedIPDAO] Error blocking IP: " + e.getMessage());
            return false;
        }
    }

    /**
     * Block an IP with expiry time
     */
    public boolean blockIPWithExpiry(String ipAddress, String reason, int blockedBy, int expiryHours) {
        String sql = "INSERT INTO blocked_ips (ip_address, reason, blocked_by, blocked_at, expires_at, is_permanent) " +
                "VALUES (?, ?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? HOUR), FALSE)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ipAddress);
            ps.setString(2, reason);
            ps.setInt(3, blockedBy);
            ps.setInt(4, expiryHours);

            int rowsInserted = ps.executeUpdate();
            System.out.println("[BlockedIPDAO] IP blocked with expiry: " + ipAddress);
            return rowsInserted > 0;

        } catch (SQLException e) {
            System.err.println("[BlockedIPDAO] Error blocking IP with expiry: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all blocked IPs
     */
    public List<BlockedIP> getAllBlockedIPs() {
        List<BlockedIP> blockedIPs = new ArrayList<>();
        String sql = "SELECT * FROM blocked_ips ORDER BY blocked_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                blockedIPs.add(mapResultSetToBlockedIP(rs));
            }

        } catch (SQLException e) {
            System.err.println("[BlockedIPDAO] Error retrieving all blocked IPs: " + e.getMessage());
        }

        return blockedIPs;
    }

    /**
     * Get active blocked IPs (not expired)
     */
    public List<BlockedIP> getActiveBlockedIPs() {
        List<BlockedIP> blockedIPs = new ArrayList<>();
        String sql = "SELECT * FROM blocked_ips WHERE is_permanent = TRUE OR expires_at IS NULL OR expires_at > NOW() ORDER BY blocked_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                blockedIPs.add(mapResultSetToBlockedIP(rs));
            }

        } catch (SQLException e) {
            System.err.println("[BlockedIPDAO] Error retrieving active blocked IPs: " + e.getMessage());
        }

        return blockedIPs;
    }

    /**
     * Check if an IP is blocked
     */
    public boolean isIPBlocked(String ipAddress) {
        String sql = "SELECT COUNT(*) as count FROM blocked_ips WHERE ip_address = ? " +
                "AND (is_permanent = TRUE OR expires_at IS NULL OR expires_at > NOW())";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ipAddress);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }

        } catch (SQLException e) {
            System.err.println("[BlockedIPDAO] Error checking if IP is blocked: " + e.getMessage());
        }

        return false;
    }

    /**
     * Unblock an IP address
     */
    public boolean unblockIP(int blockId) {
        String sql = "DELETE FROM blocked_ips WHERE block_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, blockId);
            int rowsDeleted = ps.executeUpdate();
            System.out.println("[BlockedIPDAO] IP unblocked: " + blockId);
            return rowsDeleted > 0;

        } catch (SQLException e) {
            System.err.println("[BlockedIPDAO] Error unblocking IP: " + e.getMessage());
            return false;
        }
    }

    /**
     * Unblock an IP by address
     */
    public boolean unblockIPByAddress(String ipAddress) {
        String sql = "DELETE FROM blocked_ips WHERE ip_address = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ipAddress);
            int rowsDeleted = ps.executeUpdate();
            System.out.println("[BlockedIPDAO] IP unblocked: " + ipAddress);
            return rowsDeleted > 0;

        } catch (SQLException e) {
            System.err.println("[BlockedIPDAO] Error unblocking IP by address: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get block record by IP
     */
    public BlockedIP getBlockedIPRecord(String ipAddress) {
        String sql = "SELECT * FROM blocked_ips WHERE ip_address = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ipAddress);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToBlockedIP(rs);
            }

        } catch (SQLException e) {
            System.err.println("[BlockedIPDAO] Error getting blocked IP record: " + e.getMessage());
        }

        return null;
    }

    /**
     * Clean up expired blocks
     */
    public int removeExpiredBlocks() {
        String sql = "DELETE FROM blocked_ips WHERE is_permanent = FALSE AND expires_at IS NOT NULL AND expires_at < NOW()";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            int rowsDeleted = stmt.executeUpdate(sql);
            System.out.println("[BlockedIPDAO] Removed " + rowsDeleted + " expired blocks");
            return rowsDeleted;

        } catch (SQLException e) {
            System.err.println("[BlockedIPDAO] Error removing expired blocks: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get blocked IP count
     */
    public int getBlockedIPCount() {
        String sql = "SELECT COUNT(*) as count FROM blocked_ips";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("[BlockedIPDAO] Error getting blocked IP count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Helper method to map ResultSet to BlockedIP
     */
    private BlockedIP mapResultSetToBlockedIP(ResultSet rs) throws SQLException {
        BlockedIP blockedIP = new BlockedIP();
        blockedIP.setBlockId(rs.getInt("block_id"));
        blockedIP.setIpAddress(rs.getString("ip_address"));
        blockedIP.setReason(rs.getString("reason"));
        blockedIP.setBlockedBy(rs.getInt("blocked_by"));
        blockedIP.setBlockedAt(rs.getTimestamp("blocked_at"));
        blockedIP.setExpiresAt(rs.getTimestamp("expires_at"));
        blockedIP.setPermanent(rs.getBoolean("is_permanent"));
        return blockedIP;
    }
}
