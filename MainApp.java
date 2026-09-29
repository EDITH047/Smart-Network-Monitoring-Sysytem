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
        
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }
        
        // Auto-start MySQL if it is not currently running
        startMySQLIfNecessary();

        // Auto-discover network devices for this system
        try {
            new com.networkmonitor.service.NetworkDiscoveryService().discoverAndRegister();
        } catch (Exception e) {
            System.err.println("[MainApp] Network discovery failed: " + e.getMessage());
        }

        // Register a SINGLE shutdown hook that:
        //   1. Cleans all runtime data from the database
        //   2. Closes the DB connection
        //   3. Shuts down the embedded MySQL server (if we started one)
        // Using one hook guarantees the correct order of operations.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Step 1: Clean runtime data from the database
            System.out.println("[MainApp] Cleaning runtime data from database...");
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
                st.executeUpdate("TRUNCATE TABLE audit_log");
                st.executeUpdate("TRUNCATE TABLE devices");
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
                st.close();
                System.out.println("[MainApp] Database cleaned successfully.");
            } catch (Exception e) {
                System.err.println("[MainApp] Error cleaning database: " + e.getMessage());
            }

            // Step 2: Close the DB connection
            DatabaseConfig.closeConnection();

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
                // Set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

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
            
            // Wait a few seconds for it to start up
            System.out.println("[MainApp] Waiting 3 seconds for MySQL to initialize...");
            Thread.sleep(3000);
            
            System.out.println("[MainApp] MySQL auto-start procedure completed.");
            
        } catch (Exception e) {
            System.err.println("[MainApp] Failed to start MySQL automatically: " + e.getMessage());
        }
    }
}
