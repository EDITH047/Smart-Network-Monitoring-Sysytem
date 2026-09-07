package com.networkmonitor.model;

import java.sql.Timestamp;

/**
 * BlockedIP - POJO representing a blocked/blacklisted IP address
 * Maps to: blocked_ips table
 */
public class BlockedIP {
    private int blockId;
    private String ipAddress;
    private String reason;
    private int blockedBy; // user_id
    private Timestamp blockedAt;
    private Timestamp expiresAt;
    private boolean isPermanent;

    // Constructors
    public BlockedIP() {
    }

    public BlockedIP(String ipAddress, String reason, int blockedBy, boolean isPermanent) {
        this.ipAddress = ipAddress;
        this.reason = reason;
        this.blockedBy = blockedBy;
        this.isPermanent = isPermanent;
    }

    public BlockedIP(int blockId, String ipAddress, String reason, int blockedBy,
                     Timestamp blockedAt, Timestamp expiresAt, boolean isPermanent) {
        this.blockId = blockId;
        this.ipAddress = ipAddress;
        this.reason = reason;
        this.blockedBy = blockedBy;
        this.blockedAt = blockedAt;
        this.expiresAt = expiresAt;
        this.isPermanent = isPermanent;
    }

    // Getters and Setters
    public int getBlockId() {
        return blockId;
    }

    public void setBlockId(int blockId) {
        this.blockId = blockId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getBlockedBy() {
        return blockedBy;
    }

    public void setBlockedBy(int blockedBy) {
        this.blockedBy = blockedBy;
    }

    public Timestamp getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(Timestamp blockedAt) {
        this.blockedAt = blockedAt;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isPermanent() {
        return isPermanent;
    }

    public void setPermanent(boolean permanent) {
        isPermanent = permanent;
    }

    @Override
    public String toString() {
        return "BlockedIP{" +
                "blockId=" + blockId +
                ", ipAddress='" + ipAddress + '\'' +
                ", reason='" + reason + '\'' +
                ", blockedAt=" + blockedAt +
                ", expiresAt=" + expiresAt +
                ", isPermanent=" + isPermanent +
                '}';
    }
}
