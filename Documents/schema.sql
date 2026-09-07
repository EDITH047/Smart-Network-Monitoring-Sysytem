-- ==========================================================
-- Smart Network Monitoring, Security & Optimization System
-- Database: network_monitor_db
-- Run this script in MySQL to create all tables
-- ==========================================================

CREATE DATABASE IF NOT EXISTS network_monitor_db;
USE network_monitor_db;

-- 1. Users table
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    role ENUM('ADMIN', 'OPERATOR', 'VIEWER') NOT NULL DEFAULT 'VIEWER',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL
);

-- 2. Network Devices
CREATE TABLE devices (
    device_id INT AUTO_INCREMENT PRIMARY KEY,
    device_name VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    mac_address VARCHAR(17),
    device_type ENUM('ROUTER', 'SWITCH', 'SERVER', 'ACCESS_POINT', 'FIREWALL') NOT NULL,
    location VARCHAR(100),
    status ENUM('ONLINE', 'OFFLINE', 'WARNING') DEFAULT 'OFFLINE',
    added_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (added_by) REFERENCES users(user_id) ON DELETE SET NULL
);

-- 3. Network Metrics (time-series data)
CREATE TABLE network_metrics (
    metric_id INT AUTO_INCREMENT PRIMARY KEY,
    device_id INT NOT NULL,
    bandwidth_usage DOUBLE DEFAULT 0,
    latency_ms DOUBLE DEFAULT 0,
    packet_loss_pct DOUBLE DEFAULT 0,
    packets_in BIGINT DEFAULT 0,
    packets_out BIGINT DEFAULT 0,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE
);

-- 4. Security Events
CREATE TABLE security_events (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    device_id INT NOT NULL,
    event_type ENUM('DDOS', 'BRUTE_FORCE', 'PORT_SCAN', 'MALWARE', 'UNAUTHORIZED_ACCESS') NOT NULL,
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
    source_ip VARCHAR(45),
    description TEXT,
    is_resolved BOOLEAN DEFAULT FALSE,
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE
);

-- 5. Firewall Rules
CREATE TABLE firewall_rules (
    rule_id INT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    source_ip VARCHAR(45),
    dest_ip VARCHAR(45),
    port INT,
    protocol ENUM('TCP', 'UDP', 'ICMP', 'ALL') DEFAULT 'ALL',
    action ENUM('ALLOW', 'BLOCK', 'RATE_LIMIT') NOT NULL,
    priority INT DEFAULT 100,
    is_active BOOLEAN DEFAULT TRUE,
    created_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL
);

-- 6. Alerts
CREATE TABLE alerts (
    alert_id INT AUTO_INCREMENT PRIMARY KEY,
    device_id INT NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    severity ENUM('INFO', 'WARNING', 'CRITICAL') NOT NULL,
    message TEXT NOT NULL,
    is_acknowledged BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    acknowledged_by INT NULL,
    FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE,
    FOREIGN KEY (acknowledged_by) REFERENCES users(user_id) ON DELETE SET NULL
);

-- 7. Optimization Results
CREATE TABLE optimization_results (
    result_id INT AUTO_INCREMENT PRIMARY KEY,
    device_id INT NOT NULL,
    current_bandwidth DOUBLE,
    recommended_bandwidth DOUBLE,
    optimization_score INT CHECK (optimization_score BETWEEN 0 AND 100),
    suggestion TEXT,
    analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE
);

-- 8. Blocked IPs
CREATE TABLE blocked_ips (
    block_id INT AUTO_INCREMENT PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL,
    reason VARCHAR(255),
    blocked_by INT,
    blocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    is_permanent BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (blocked_by) REFERENCES users(user_id) ON DELETE SET NULL
);

-- 9. Audit Log
CREATE TABLE audit_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    ip_address VARCHAR(45),
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- ==========================================================
-- Indexes for performance
-- ==========================================================
CREATE INDEX idx_metrics_device_time ON network_metrics(device_id, recorded_at);
CREATE INDEX idx_events_device ON security_events(device_id, detected_at);
CREATE INDEX idx_alerts_device ON alerts(device_id, created_at);
CREATE INDEX idx_audit_user ON audit_log(user_id, performed_at);
CREATE INDEX idx_devices_status ON devices(status);
CREATE INDEX idx_blocked_ip ON blocked_ips(ip_address);

-- ==========================================================
-- Insert default admin user (password: admin123)
-- SHA-256 hash of "admin123"
-- ==========================================================
INSERT INTO users (username, password_hash, full_name, email, role)
VALUES ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
        'System Administrator', 'admin@network.local', 'ADMIN');

-- ==========================================================
-- Sample devices for testing
-- ==========================================================
INSERT INTO devices (device_name, ip_address, mac_address, device_type, location, status, added_by) VALUES
('Core Router', '192.168.1.1', 'AA:BB:CC:DD:EE:01', 'ROUTER', 'Server Room A', 'ONLINE', 1),
('Main Switch', '192.168.1.2', 'AA:BB:CC:DD:EE:02', 'SWITCH', 'Server Room A', 'ONLINE', 1),
('Web Server', '192.168.1.10', 'AA:BB:CC:DD:EE:03', 'SERVER', 'Data Center', 'ONLINE', 1),
('Wi-Fi AP Floor 1', '192.168.1.20', 'AA:BB:CC:DD:EE:04', 'ACCESS_POINT', 'Floor 1', 'WARNING', 1),
('Edge Firewall', '192.168.1.254', 'AA:BB:CC:DD:EE:05', 'FIREWALL', 'DMZ', 'ONLINE', 1);
