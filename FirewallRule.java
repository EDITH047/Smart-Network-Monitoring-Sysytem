package com.networkmonitor.model;

import java.sql.Timestamp;

/**
 * FirewallRule - POJO representing a firewall rule
 * Maps to: firewall_rules table
 */
public class FirewallRule {
    private int ruleId;
    private String ruleName;
    private String sourceIp;
    private String destIp;
    private int port;
    private String protocol; // TCP, UDP, ICMP, ALL
    private String action; // ALLOW, BLOCK, RATE_LIMIT
    private int priority;
    private boolean isActive;
    private int createdBy; // user_id
    private Timestamp createdAt;

    // Constructors
    public FirewallRule() {
    }

    public FirewallRule(String ruleName, String sourceIp, String destIp, int port,
                        String protocol, String action, int priority, int createdBy) {
        this.ruleName = ruleName;
        this.sourceIp = sourceIp;
        this.destIp = destIp;
        this.port = port;
        this.protocol = protocol;
        this.action = action;
        this.priority = priority;
        this.isActive = true;
        this.createdBy = createdBy;
    }

    public FirewallRule(int ruleId, String ruleName, String sourceIp, String destIp, int port,
                        String protocol, String action, int priority, boolean isActive,
                        int createdBy, Timestamp createdAt) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.sourceIp = sourceIp;
        this.destIp = destIp;
        this.port = port;
        this.protocol = protocol;
        this.action = action;
        this.priority = priority;
        this.isActive = isActive;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getRuleId() {
        return ruleId;
    }

    public void setRuleId(int ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getDestIp() {
        return destIp;
    }

    public void setDestIp(String destIp) {
        this.destIp = destIp;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "FirewallRule{" +
                "ruleId=" + ruleId +
                ", ruleName='" + ruleName + '\'' +
                ", sourceIp='" + sourceIp + '\'' +
                ", destIp='" + destIp + '\'' +
                ", port=" + port +
                ", protocol='" + protocol + '\'' +
                ", action='" + action + '\'' +
                ", priority=" + priority +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }
}
