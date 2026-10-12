package com.networkmonitor.main;

import com.networkmonitor.ui.LoginFrame;
import com.networkmonitor.config.DatabaseConfig;
import javax.swing.*;
import java.io.File;
import java.net.Socket;
import java.sql.Connection;
import java.sql.Statement;

/**
 * MainApp - Application entry point
 * Starts the Smart Network Monitoring System
 */
public class MainApp {

    // Reference to the embedded MySQL process (if we started one)
    private static Process mysqlProcess = null;

    public static void main(String[] args) {
        // Set system properties
        System.setProperty("java.awt.headless", "false");
        
        // Initialize theme with user's persisted preference (Dark or Light)
        com.networkmonitor.util.UITheme.initTheme();
        
        // Auto-start MySQL if it is not currently running
        startMySQLIfNecessary();

        // Ensure database tables and default admin user exist
        ensureDatabaseAndAdmin();

        // Auto-discover network devices for this system
        try {
            new com.networkmonitor.service.NetworkDiscoveryService().discoverAndRegister();
        } catch (Exception e) {
            System.err.println("[MainApp] Network discovery failed: " + e.getMessage());
        }

        // Register a SINGLE shutdown hook that:
        //   1. Cleans runtime data from the database (preserves audit_log and users)
        //   2. Closes the DB connection
        //   3. Shuts down the embedded MySQL server (if we started one)
        // Using one hook guarantees the correct order of operations.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Step 1: Clean runtime data from the database (keep users & audit_log intact)
            System.out.println("[MainApp] Cleaning runtime telemetry data from database...");
            try {
                Connection conn = DatabaseConfig.getConnection();
                Statement st = conn.createStatement();
                // Disable FK checks to allow deleting in any order
                st.execute("SET FOREIGN_KEY_CHECKS = 0");
                st.executeUpdate("TRUNCATE TABLE network_metrics");
                st.executeUpdate("TRUNCATE TABLE alerts");
                st.executeUpdate("TRUNCATE TABLE security_events");
                st.executeUpdate("TRUNCATE TABLE optimization_results");
                st.executeUpdate("TRUNCATE TABLE blocked_ips");
                st.executeUpdate("TRUNCATE TABLE firewall_rules");
                // Do NOT truncate audit_log so authentication & audit history is preserved
                st.executeUpdate("TRUNCATE TABLE devices");
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
                st.close();
                System.out.println("[MainApp] Database cleaned successfully.");
            } catch (Exception e) {
                System.err.println("[MainApp] Error cleaning database: " + e.getMessage());
            }

            // Step 2: Shutdown Connection Pool
            DatabaseConfig.shutdown();

            // Step 3: Shut down embedded MySQL server
            if (mysqlProcess != null && mysqlProcess.isAlive()) {
                System.out.println("[MainApp] Shutting down embedded MySQL server...");
                mysqlProcess.destroy();
                try {
                    Thread.sleep(3000);
                    if (mysqlProcess.isAlive()) {
                        mysqlProcess.destroyForcibly();
                    }
                } catch (InterruptedException ex) {
                    mysqlProcess.destroyForcibly();
                }
                System.out.println("[MainApp] MySQL server stopped.");
            }
        }, "ShutdownHook"));

        // Launch on EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            try {

                // Display splash screen info
                System.out.println("================================================================================");
                System.out.println("Smart Network Monitoring, Security & Optimization System");
                System.out.println("Version 1.0");
                System.out.println("================================================================================");
                System.out.println("");

                // Display demo credentials
                System.out.println("\uD83D\uDCCB Default Credentials:");
                System.out.println("   Username: admin");
                System.out.println("   Password: admin123");
                System.out.println("");

                System.out.println("\uD83D\uDE80 Launching Login Frame...");
                System.out.println("");

                // Create and show login frame
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);

            } catch (Exception e) {
                System.err.println("Error starting application: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

    /**
     * Checks if MySQL is running on port 3306. If not, attempts to start it
     * from common installation paths using the local mysql-data directory.
     */
    private static void startMySQLIfNecessary() {
        System.out.println("[MainApp] Checking MySQL status on port 3306...");
        
        // Check if port 3306 is already in use
        try (Socket socket = new Socket("localhost", 3306)) {
            System.out.println("[MainApp] MySQL is already running on port 3306.");
            return;
        } catch (Exception e) {
            System.out.println("[MainApp] MySQL is not running on port 3306. Attempting to start embedded instance...");
        }

        // Possible paths to mysqld.exe
        String[] possiblePaths = {
            "C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysqld.exe",
            "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqld.exe",
            "C:\\Program Files\\MySQL\\MySQL Server 8.1\\bin\\mysqld.exe",
            "C:\\Program Files\\MariaDB 10.11\\bin\\mysqld.exe",
            "C:\\xampp\\mysql\\bin\\mysqld.exe"
        };

        String mysqldPath = null;
        for (String path : possiblePaths) {
            if (new File(path).exists()) {
                mysqldPath = path;
                break;
            }
        }

        if (mysqldPath == null) {
            System.err.println("[MainApp] WARNING: Could not find mysqld.exe in standard locations.");
            System.err.println("[MainApp] You may need to start MySQL manually.");
            return;
        }

        // Get the absolute path to the local data directory
        String dataDir = new File("mysql-data").getAbsolutePath();
        if (!new File(dataDir).exists()) {
            System.err.println("[MainApp] WARNING: Local mysql-data directory not found at " + dataDir);
        }

        try {
            System.out.println("[MainApp] Starting MySQL server from: " + mysqldPath);
            System.out.println("[MainApp] Using data directory: " + dataDir);
            
            ProcessBuilder pb = new ProcessBuilder(mysqldPath, "--datadir=" + dataDir, "--port=3306", "--console", "--skip-log-bin");
            
            // Redirect output to a log file so it doesn't clutter the console indefinitely
            File logFile = new File("mysql_startup.log");
            pb.redirectErrorStream(true);
            pb.redirectOutput(logFile);
            
            // Start the process and store the reference for the shutdown hook
            mysqlProcess = pb.start();
            
            // Actively poll until MySQL port is open and accepting socket connections (up to 15s)
            System.out.println("[MainApp] Waiting for MySQL to initialize on port 3306...");
            boolean ready = false;
            for (int i = 0; i < 30; i++) {
                try (Socket s = new Socket("localhost", 3306)) {
                    ready = true;
                    System.out.println("[MainApp] MySQL is ready on port 3306.");
                    break;
                } catch (Exception ex) {
                    Thread.sleep(500);
                }
            }
            if (!ready) {
                System.err.println("[MainApp] WARNING: Timed out waiting for MySQL to respond on port 3306.");
            }
            
            System.out.println("[MainApp] MySQL auto-start procedure completed.");
            
        } catch (Exception e) {
            System.err.println("[MainApp] Failed to start MySQL automatically: " + e.getMessage());
        }
    }

    /**
     * Ensures users and audit_log tables exist and seeds the default admin user
     * (admin / admin123) if missing.
     */
    private static void ensureDatabaseAndAdmin() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement st = conn.createStatement()) {

            // 1. Users
            st.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "full_name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100), " +
                    "role ENUM('ADMIN', 'OPERATOR', 'VIEWER') NOT NULL DEFAULT 'VIEWER', " +
                    "is_active BOOLEAN DEFAULT TRUE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "last_login TIMESTAMP NULL)");

            // 2. Devices
            st.execute("CREATE TABLE IF NOT EXISTS devices (" +
                    "device_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "device_name VARCHAR(100) NOT NULL, " +
                    "ip_address VARCHAR(45) NOT NULL, " +
                    "mac_address VARCHAR(17), " +
                    "device_type VARCHAR(50) NOT NULL, " +
                    "location VARCHAR(100), " +
                    "status ENUM('ONLINE', 'OFFLINE', 'WARNING', 'CRITICAL') DEFAULT 'OFFLINE', " +
                    "network_interface VARCHAR(50), " +
                    "description TEXT, " +
                    "last_seen TIMESTAMP NULL, " +
                    "added_by INT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (added_by) REFERENCES users(user_id) ON DELETE SET NULL)");

            // 3. Network Metrics
            st.execute("CREATE TABLE IF NOT EXISTS network_metrics (" +
                    "metric_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "device_id INT NOT NULL, " +
                    "bandwidth_usage DOUBLE DEFAULT 0, " +
                    "latency_ms DOUBLE DEFAULT 0, " +
                    "packet_loss_pct DOUBLE DEFAULT 0, " +
                    "packets_in BIGINT DEFAULT 0, " +
                    "packets_out BIGINT DEFAULT 0, " +
                    "recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE)");

            // 4. Security Events
            st.execute("CREATE TABLE IF NOT EXISTS security_events (" +
                    "event_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "device_id INT NOT NULL, " +
                    "event_type ENUM('DDOS', 'BRUTE_FORCE', 'PORT_SCAN', 'MALWARE', 'UNAUTHORIZED_ACCESS') NOT NULL, " +
                    "severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL, " +
                    "source_ip VARCHAR(45), " +
                    "description TEXT, " +
                    "is_resolved BOOLEAN DEFAULT FALSE, " +
                    "detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "resolved_at TIMESTAMP NULL, " +
                    "FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE)");

            // 5. Firewall Rules
            st.execute("CREATE TABLE IF NOT EXISTS firewall_rules (" +
                    "rule_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "rule_name VARCHAR(100) NOT NULL, " +
                    "source_ip VARCHAR(45), " +
                    "dest_ip VARCHAR(45), " +
                    "port INT, " +
                    "protocol ENUM('TCP', 'UDP', 'ICMP', 'ALL') DEFAULT 'ALL', " +
                    "action ENUM('ALLOW', 'BLOCK', 'RATE_LIMIT') NOT NULL, " +
                    "priority INT DEFAULT 100, " +
                    "is_active BOOLEAN DEFAULT TRUE, " +
                    "created_by INT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL)");

            // 6. Alerts
            st.execute("CREATE TABLE IF NOT EXISTS alerts (" +
                    "alert_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "device_id INT NOT NULL, " +
                    "alert_type VARCHAR(50) NOT NULL, " +
                    "severity ENUM('INFO', 'WARNING', 'CRITICAL') NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "is_acknowledged BOOLEAN DEFAULT FALSE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "acknowledged_by INT NULL, " +
                    "FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (acknowledged_by) REFERENCES users(user_id) ON DELETE SET NULL)");

            // 7. Optimization Results
            st.execute("CREATE TABLE IF NOT EXISTS optimization_results (" +
                    "result_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "device_id INT NOT NULL, " +
                    "current_bandwidth DOUBLE, " +
                    "recommended_bandwidth DOUBLE, " +
                    "optimization_score INT, " +
                    "suggestion TEXT, " +
                    "analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE)");

            // 8. Blocked IPs
            st.execute("CREATE TABLE IF NOT EXISTS blocked_ips (" +
                    "block_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "ip_address VARCHAR(45) NOT NULL, " +
                    "reason VARCHAR(255), " +
                    "blocked_by INT, " +
                    "blocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "expires_at TIMESTAMP NULL, " +
                    "is_permanent BOOLEAN DEFAULT FALSE, " +
                    "FOREIGN KEY (blocked_by) REFERENCES users(user_id) ON DELETE SET NULL)");

            // 9. Audit Log
            st.execute("CREATE TABLE IF NOT EXISTS audit_log (" +
                    "log_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT, " +
                    "action VARCHAR(100) NOT NULL, " +
                    "details TEXT, " +
                    "ip_address VARCHAR(45), " +
                    "performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL)");

            com.networkmonitor.dao.UserDAO userDAO = new com.networkmonitor.dao.UserDAO();
            if (userDAO.findByUsername("admin") == null) {
                System.out.println("[MainApp] Admin user not found. Seeding default admin user...");
                com.networkmonitor.model.User defaultAdmin = new com.networkmonitor.model.User(
                    "admin",
                    "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9",
                    "System Administrator",
                    "admin@network.local",
                    "ADMIN"
                );
                userDAO.insertUser(defaultAdmin);
                System.out.println("[MainApp] Default admin user (admin / admin123) seeded successfully.");
            } else {
                System.out.println("[MainApp] Admin user verified in database.");
            }

            // Ensure performance indexes exist for telemetry and optimization
            ensureIndex(conn, "network_metrics", "idx_metrics_time", "CREATE INDEX idx_metrics_time ON network_metrics(recorded_at)");
            ensureIndex(conn, "optimization_results", "idx_opt_device_time", "CREATE INDEX idx_opt_device_time ON optimization_results(device_id, analyzed_at)");

        } catch (Exception e) {
            System.err.println("[MainApp] Database/Admin initialization check failed: " + e.getMessage());
        }
    }

    /**
     * Checks whether an index exists on a table and creates it if missing.
     */
    private static void ensureIndex(Connection conn, String tableName, String indexName, String createSql) {
        try {
            java.sql.DatabaseMetaData meta = conn.getMetaData();
            try (java.sql.ResultSet rs = meta.getIndexInfo(null, null, tableName, false, false)) {
                boolean exists = false;
                while (rs.next()) {
                    if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    try (Statement st = conn.createStatement()) {
                        st.execute(createSql);
                        System.out.println("[MainApp] Created index " + indexName + " on " + tableName);
                    }
                } else {
                    System.out.println("[MainApp] Index " + indexName + " verified on " + tableName);
                }
            }
        } catch (Exception e) {
            System.err.println("[MainApp] Note verifying index " + indexName + ": " + e.getMessage());
        }
    }
}
