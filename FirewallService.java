package com.networkmonitor.service;

import com.networkmonitor.dao.FirewallRuleDAO;
import com.networkmonitor.model.FirewallRule;
import com.networkmonitor.util.ValidationUtil;
import java.util.List;

/**
 * FirewallService - Handles firewall rule management and validation
 * Uses: FirewallRuleDAO, ValidationUtil
 */
public class FirewallService {

    private static final FirewallService instance = new FirewallService();
    private FirewallRuleDAO ruleDAO = new FirewallRuleDAO();

    private FirewallService() {
    }

    public static FirewallService getInstance() {
        return instance;
    }

    /**
     * Add a new firewall rule with validation
     */
    public boolean addRule(String ruleName, String sourceIp, String destIp, int port,
                          String protocol, String action, int priority, int createdBy) {

        // Validate inputs
        if (!validateRule(sourceIp, destIp, port)) {
            System.out.println("[FirewallService] Rule validation failed");
            return false;
        }

        // Check if rule name already exists
        if (ruleDAO.ruleExists(ruleName)) {
            System.out.println("[FirewallService] Rule name already exists: " + ruleName);
            return false;
        }

        // Create and add rule
        FirewallRule rule = new FirewallRule(ruleName, sourceIp, destIp, port, protocol, action, priority, createdBy);
        boolean success = ruleDAO.addRule(rule);

        if (success) {
            System.out.println("[FirewallService] Rule added: " + ruleName + " (Priority: " + priority + ")");
        }

        return success;
    }

    /**
     * Validate firewall rule parameters
     */
    private boolean validateRule(String sourceIp, String destIp, int port) {
        // Validate IPs (can be null, empty, *, or ANY for any)
        if (sourceIp != null && !sourceIp.isEmpty() && !sourceIp.equals("*") && !sourceIp.equalsIgnoreCase("ANY") && !ValidationUtil.isValidIPv4(sourceIp)) {
            System.out.println("[FirewallService] Invalid source IP: " + sourceIp);
            return false;
        }

        if (destIp != null && !destIp.isEmpty() && !destIp.equals("*") && !destIp.equalsIgnoreCase("ANY") && !ValidationUtil.isValidIPv4(destIp)) {
            System.out.println("[FirewallService] Invalid destination IP: " + destIp);
            return false;
        }

        // Validate port (0 means ANY)
        if (port > 0 && !ValidationUtil.isValidPort(port)) {
            System.out.println("[FirewallService] Invalid port: " + port);
            return false;
        }

        return true;
    }

    /**
     * Get all firewall rules
     */
    public List<FirewallRule> getAllRules() {
        return ruleDAO.getAllRules();
    }

    /**
     * Get active firewall rules only
     */
    public List<FirewallRule> getActiveRules() {
        return ruleDAO.getActiveRules();
    }

    /**
     * Update a firewall rule
     */
    public boolean updateRule(FirewallRule rule) {
        if (!validateRule(rule.getSourceIp(), rule.getDestIp(), rule.getPort())) {
            return false;
        }

        boolean success = ruleDAO.updateRule(rule);

        if (success) {
            System.out.println("[FirewallService] Rule updated: " + rule.getRuleName());
        }

        return success;
    }

    /**
     * Toggle rule active/inactive
     */
    public boolean toggleRuleActive(int ruleId) {
        return ruleDAO.toggleRuleActive(ruleId);
    }

    /**
     * Set rule active status
     */
    public boolean setRuleActive(int ruleId, boolean active) {
        return ruleDAO.setRuleActive(ruleId, active);
    }

    /**
     * Delete a firewall rule
     */
    public boolean deleteRule(int ruleId) {
        boolean success = ruleDAO.deleteRule(ruleId);

        if (success) {
            System.out.println("[FirewallService] Rule deleted: " + ruleId);
        }

        return success;
    }

    /**
     * Get rules by action type
     */
    public List<FirewallRule> getRulesByAction(String action) {
        return ruleDAO.getRulesByAction(action);
    }

    /**
     * Get BLOCK rules
     */
    public List<FirewallRule> getBlockRules() {
        return getRulesByAction("BLOCK");
    }

    /**
     * Get ALLOW rules
     */
    public List<FirewallRule> getAllowRules() {
        return getRulesByAction("ALLOW");
    }

    /**
     * Get RATE_LIMIT rules
     */
    public List<FirewallRule> getRateLimitRules() {
        return getRulesByAction("RATE_LIMIT");
    }

    /**
     * Apply firewall rules (check if traffic is allowed)
     */
    public boolean isTrafficAllowed(String sourceIp, String destIp, int port) {
        return isTrafficAllowed(sourceIp, destIp, port, null);
    }

    /**
     * Apply firewall rules with protocol check
     */
    public boolean isTrafficAllowed(String sourceIp, String destIp, int port, String protocol) {
        List<FirewallRule> rules = getActiveRules();

        for (FirewallRule rule : rules) {
            // Check if rule matches
            boolean matches = ruleMatches(rule, sourceIp, destIp, port, protocol);

            if (matches) {
                if ("ALLOW".equalsIgnoreCase(rule.getAction())) {
                    return true;
                } else if ("BLOCK".equalsIgnoreCase(rule.getAction()) || "DENY".equalsIgnoreCase(rule.getAction())) {
                    return false;
                } else if ("RATE_LIMIT".equalsIgnoreCase(rule.getAction())) {
                    // Allowed under rate limiter
                    return true;
                }
            }
        }

        // Default allow if no rule matches
        return true;
    }

    /**
     * Check if traffic matches a firewall rule with wildcard support
     */
    private boolean ruleMatches(FirewallRule rule, String sourceIp, String destIp, int port, String protocol) {
        // Check source IP
        if (rule.getSourceIp() != null && !rule.getSourceIp().isEmpty()
                && !rule.getSourceIp().equals("*") && !rule.getSourceIp().equalsIgnoreCase("ANY")) {
            if (sourceIp == null || !rule.getSourceIp().equals(sourceIp)) {
                return false;
            }
        }

        // Check dest IP
        if (rule.getDestIp() != null && !rule.getDestIp().isEmpty()
                && !rule.getDestIp().equals("*") && !rule.getDestIp().equalsIgnoreCase("ANY")) {
            if (destIp == null || !rule.getDestIp().equals(destIp)) {
                return false;
            }
        }

        // Check port
        if (rule.getPort() > 0 && rule.getPort() != port) {
            return false;
        }

        // Check protocol
        if (protocol != null && rule.getProtocol() != null && !rule.getProtocol().isEmpty()
                && !rule.getProtocol().equals("*") && !rule.getProtocol().equalsIgnoreCase("ANY")) {
            if (!rule.getProtocol().equalsIgnoreCase(protocol)) {
                return false;
            }
        }

        return true;
    }

    public void reloadRules() {
        System.out.println("[FirewallService] Firewall rules cache refreshed.");
    }
}
