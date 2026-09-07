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

    private static FirewallService instance;
    private FirewallRuleDAO ruleDAO = new FirewallRuleDAO();

    private FirewallService() {
    }

    public static FirewallService getInstance() {
        if (instance == null) {
            instance = new FirewallService();
        }
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
        // Validate IPs (can be null for any)
        if (sourceIp != null && !sourceIp.isEmpty() && !ValidationUtil.isValidIPv4(sourceIp)) {
            System.out.println("[FirewallService] Invalid source IP: " + sourceIp);
            return false;
        }

        if (destIp != null && !destIp.isEmpty() && !ValidationUtil.isValidIPv4(destIp)) {
            System.out.println("[FirewallService] Invalid destination IP: " + destIp);
            return false;
        }

        // Validate port
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
     * Apply firewall rules (check if traffic is allowed)
     */
    public boolean isTrafficAllowed(String sourceIp, String destIp, int port) {
        List<FirewallRule> rules = getActiveRules();

        for (FirewallRule rule : rules) {
            // Check if rule matches
            boolean matches = ruleMatches(rule, sourceIp, destIp, port);

            if (matches) {
                if ("ALLOW".equals(rule.getAction())) {
                    return true;
                } else if ("BLOCK".equals(rule.getAction())) {
                    return false;
                }
            }
        }

        // Default allow if no rule matches
        return true;
    }

    /**
     * Check if traffic matches a firewall rule
     */
    private boolean ruleMatches(FirewallRule rule, String sourceIp, String destIp, int port) {
        // Check source IP
        if (rule.getSourceIp() != null && !rule.getSourceIp().isEmpty() && !rule.getSourceIp().equals(sourceIp)) {
            return false;
        }

        // Check dest IP
        if (rule.getDestIp() != null && !rule.getDestIp().isEmpty() && !rule.getDestIp().equals(destIp)) {
            return false;
        }

        // Check port
        if (rule.getPort() > 0 && rule.getPort() != port) {
            return false;
        }

        return true;
    }
}
