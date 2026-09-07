package com.networkmonitor.model;

import java.sql.Timestamp;

/**
 * NetworkMetric - POJO representing network performance metrics for a device
 * Maps to: network_metrics table
 */
public class NetworkMetric {
    private int metricId;
    private int deviceId;
    private double bandwidthUsage;
    private double latencyMs;
    private double packetLossPct;
    private long packetsIn;
    private long packetsOut;
    private Timestamp recordedAt;

    // Constructors
    public NetworkMetric() {
    }

    public NetworkMetric(int deviceId, double bandwidthUsage, double latencyMs,
                         double packetLossPct, long packetsIn, long packetsOut) {
        this.deviceId = deviceId;
        this.bandwidthUsage = bandwidthUsage;
        this.latencyMs = latencyMs;
        this.packetLossPct = packetLossPct;
        this.packetsIn = packetsIn;
        this.packetsOut = packetsOut;
    }

    public NetworkMetric(int metricId, int deviceId, double bandwidthUsage, double latencyMs,
                         double packetLossPct, long packetsIn, long packetsOut, Timestamp recordedAt) {
        this.metricId = metricId;
        this.deviceId = deviceId;
        this.bandwidthUsage = bandwidthUsage;
        this.latencyMs = latencyMs;
        this.packetLossPct = packetLossPct;
        this.packetsIn = packetsIn;
        this.packetsOut = packetsOut;
        this.recordedAt = recordedAt;
    }

    // Getters and Setters
    public int getMetricId() {
        return metricId;
    }

    public void setMetricId(int metricId) {
        this.metricId = metricId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public double getBandwidthUsage() {
        return bandwidthUsage;
    }

    public void setBandwidthUsage(double bandwidthUsage) {
        this.bandwidthUsage = bandwidthUsage;
    }

    public double getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(double latencyMs) {
        this.latencyMs = latencyMs;
    }

    public double getPacketLossPct() {
        return packetLossPct;
    }

    public void setPacketLossPct(double packetLossPct) {
        this.packetLossPct = packetLossPct;
    }

    public long getPacketsIn() {
        return packetsIn;
    }

    public void setPacketsIn(long packetsIn) {
        this.packetsIn = packetsIn;
    }

    public long getPacketsOut() {
        return packetsOut;
    }

    public void setPacketsOut(long packetsOut) {
        this.packetsOut = packetsOut;
    }

    public Timestamp getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Timestamp recordedAt) {
        this.recordedAt = recordedAt;
    }

    @Override
    public String toString() {
        return "NetworkMetric{" +
                "metricId=" + metricId +
                ", deviceId=" + deviceId +
                ", bandwidthUsage=" + bandwidthUsage +
                ", latencyMs=" + latencyMs +
                ", packetLossPct=" + packetLossPct +
                ", packetsIn=" + packetsIn +
                ", packetsOut=" + packetsOut +
                ", recordedAt=" + recordedAt +
                '}';
    }
}
