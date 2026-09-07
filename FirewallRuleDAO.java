package com.networkmonitor.dao;

import com.networkmonitor.config.DatabaseConfig;
import com.networkmonitor.model.FirewallRule;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FirewallRuleDAO - Data Access Object for FirewallRule table
 * Handles all JDBC operations related to firewall rules
 */
public class FirewallRuleDAO {

    /**
     * Add a new firewall rule
     */
    public boolean addRule(FirewallRule rule) {
        String sql = "INSERT INTO firewall_rules (rule_name, source_ip, dest_ip, port, protocol, action, priority, is_active, created_by, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, rule.getRuleName());
            ps.setString(2, rule.getSourceIp());
            ps.setString(3, rule.getDestIp());
            ps.setInt(4, rule.getPort());
            ps.setString(5, rule.getProtocol());
            ps.setString(6, rule.getAction());
            ps.setInt(7, rule.getPriority());
            ps.setBoolean(8, rule.isActive());
            ps.setInt(9, rule.getCreatedBy());

            int rowsInserted = ps.executeUpdate();
            System.out.println("[FirewallRuleDAO] Rule added: " + rule.getRuleName());
            return rowsInserted > 0;

        } catch (SQLException e) {
            System.err.println("[FirewallRuleDAO] Error adding rule: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all firewall rules ordered by priority
     */
    public List<FirewallRule> getAllRules() {
        List<FirewallRule> rules = new ArrayList<>();
        String sql = "SELECT * FROM firewall_rules ORDER BY priority ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                rules.add(mapResultSetToRule(rs));
            }

        } catch (SQLException e) {
            System.err.println("[FirewallRuleDAO] Error retrieving all rules: " + e.getMessage());
        }

        return rules;
    }

    /**
     * Get active firewall rules
     */
    public List<FirewallRule> getActiveRules() {
        List<FirewallRule> rules = new ArrayList<>();
        String sql = "SELECT * FROM firewall_rules WHERE is_active = TRUE ORDER BY priority ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                rules.add(mapResultSetToRule(rs));
            }

        } catch (SQLException e) {
            System.err.println("[FirewallRuleDAO] Error retrieving active rules: " + e.getMessage());
        }

        return rules;
    }

    /**
     * Get rule by ID
     */
    public FirewallRule getRuleById(int ruleId) {
        String sql = "SELECT * FROM firewall_rules WHERE rule_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ruleId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToRule(rs);
            }

        } catch (SQLException e) {
            System.err.println("[FirewallRuleDAO] Error getting rule by ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Update a firewall rule
     */
    public boolean updateRule(FirewallRule rule) {
        String sql = "UPDATE firewall_rules SET rule_name = ?, source_ip = ?, dest_ip = ?, port = ?, protocol = ?, action = ?, priority = ? WHERE rule_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, rule.getRuleName());
            ps.setString(2, rule.getSourceIp());
            ps.setString(3, rule.getDestIp());
            ps.setInt(4, rule.getPort());
            ps.setString(5, rule.getProtocol());
            ps.setString(6, rule.getAction());
            ps.setInt(7, rule.getPriority());
            ps.setInt(8, rule.getRuleId());

            int rowsUpdated = ps.executeUpdate();
            System.out.println("[FirewallRuleDAO] Rule updated: " + rule.getRuleName());
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("[FirewallRuleDAO] Error updating rule: " + e.getMessage());
            return false;
        }
    }

    /**
     * Toggle rule active status
     */
    public boolean toggleRuleActive(int ruleId) {
        String sql = "UPDATE firewall_rules SET is_active = !is_active WHERE rule_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ruleId);
            int rowsUpdated = ps.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("[FirewallRuleDAO] Error toggling rule: " + e.getMessage());
            return false;
        }
    }

    /**
     * Set rule active/inactive
     */
    public boolean setRuleActive(int ruleId, boolean active) {
        String sql = "UPDATE firewall_rules SET is_active = ? WHERE rule_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, active);
            ps.setInt(2, ruleId);

            int rowsUpdated = ps.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("[FirewallRuleDAO] Error setting rule active: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete a firewall rule
     */
    public boolean deleteRule(int ruleId) {
        String sql = "DELETE FROM firewall_rules WHERE rule_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ruleId);
            int rowsDeleted = ps.executeUpdate();
            System.out.println("[FirewallRuleDAO] Rule deleted: " + ruleId);
            return rowsDeleted > 0;

        } catch (SQLException e) {
            System.err.println("[FirewallRuleDAO] Error deleting rule: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get rules by action
     */
    public List<FirewallRule> getRulesByAction(String action) {
        List<FirewallRule> rules = new ArrayList<>();
        String sql = "SELECT * FROM firewall_rules WHERE action = ? ORDER BY priority";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, action);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                rules.add(mapResultSetToRule(rs));
            }

        } catch (SQLException e) {
            System.err.println("[FirewallRuleDAO] Error getting rules by action: " + e.getMessage());
        }

        return rules;
    }

    /**
     * Check if rule exists by name
     */
    public boolean ruleExists(String ruleName) {
        String sql = "SELECT COUNT(*) as count FROM firewall_rules WHERE rule_name = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ruleName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }

        } catch (SQLException e) {
            System.err.println("[FirewallRuleDAO] Error checking if rule exists: " + e.getMessage());
        }

        return false;
    }

    /**
     * Helper method to map ResultSet to FirewallRule
     */
    private FirewallRule mapResultSetToRule(ResultSet rs) throws SQLException {
        FirewallRule rule = new FirewallRule();
        rule.setRuleId(rs.getInt("rule_id"));
        rule.setRuleName(rs.getString("rule_name"));
        rule.setSourceIp(rs.getString("source_ip"));
        rule.setDestIp(rs.getString("dest_ip"));
        rule.setPort(rs.getInt("port"));
        rule.setProtocol(rs.getString("protocol"));
        rule.setAction(rs.getString("action"));
        rule.setPriority(rs.getInt("priority"));
        rule.setActive(rs.getBoolean("is_active"));
        rule.setCreatedBy(rs.getInt("created_by"));
        rule.setCreatedAt(rs.getTimestamp("created_at"));
        return rule;
    }
}
