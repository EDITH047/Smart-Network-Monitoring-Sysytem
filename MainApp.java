package com.networkmonitor.main;

import com.networkmonitor.ui.LoginFrame;
import javax.swing.*;
import java.io.File;
import java.net.Socket;

/**
 * MainApp - Application entry point
 * Starts the Smart Network Monitoring System
 */
public class MainApp {

    public static void main(String[] args) {
        // Set system properties
        System.setProperty("java.awt.headless", "false");
        
        // Auto-start MySQL if it is not currently running
        startMySQLIfNecessary();

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
                System.out.println("📋 Default Credentials:");
                System.out.println("   Username: admin");
                System.out.println("   Password: admin123");
                System.out.println("");

                System.out.println("🚀 Launching Login Frame...");
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
            
            ProcessBuilder pb = new ProcessBuilder(mysqldPath, "--datadir=" + dataDir, "--port=3306", "--console");
            
            // Redirect output to a log file so it doesn't clutter the console indefinitely
            File logFile = new File("mysql_startup.log");
            pb.redirectErrorStream(true);
            pb.redirectOutput(logFile);
            
            // Start the process
            Process mysqlProcess = pb.start();
            
            // Wait a few seconds for it to start up
            System.out.println("[MainApp] Waiting 3 seconds for MySQL to initialize...");
            Thread.sleep(3000);
            
            // Add shutdown hook to stop MySQL when the app exits
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[MainApp] Shutting down embedded MySQL server...");
                if (mysqlProcess != null && mysqlProcess.isAlive()) {
                    mysqlProcess.destroy();
                    try {
                        // Give it 3 seconds to shutdown gracefully, then forcefully kill
                        Thread.sleep(3000);
                        if (mysqlProcess.isAlive()) {
                            mysqlProcess.destroyForcibly();
                        }
                    } catch (InterruptedException ex) {
                        mysqlProcess.destroyForcibly();
                    }
                }
            }));
            
            System.out.println("[MainApp] MySQL auto-start procedure completed.");
            
        } catch (Exception e) {
            System.err.println("[MainApp] Failed to start MySQL automatically: " + e.getMessage());
        }
    }
}
