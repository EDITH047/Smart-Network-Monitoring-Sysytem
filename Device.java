package com.networkmonitor.model;

import java.sql.Timestamp;

/**
 * Device - POJO representing a network device
 * Maps to: devices table
 */
public class Device {
    private int deviceId;
    private String deviceName;
    private String ipAddress;
    private String macAddress;
    private String deviceType; // ROUTER, SWITCH, SERVER, ACCESS_POINT, FIREWALL
    private String location;
    private String status; // ONLINE, OFFLINE, WARNING
    private int addedBy; // user_id who added this device
    private Timestamp createdAt;

    // Constructors
    public Device() {
    }

    public Device(String deviceName, String ipAddress, String macAddress, String deviceType,
                  String location, int addedBy) {
        this.deviceName = deviceName;
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
        this.deviceType = deviceType;
        this.location = location;
        this.status = "OFFLINE";
        this.addedBy = addedBy;
    }

    public Device(int deviceId, String deviceName, String ipAddress, String macAddress,
                  String deviceType, String location, String status, int addedBy, Timestamp createdAt) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
        this.deviceType = deviceType;
        this.location = location;
        this.status = status;
        this.addedBy = addedBy;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(int addedBy) {
        this.addedBy = addedBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Device{" +
                "deviceId=" + deviceId +
                ", deviceName='" + deviceName + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", macAddress='" + macAddress + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", location='" + location + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
