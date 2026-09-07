package com.networkmonitor.main;

import com.networkmonitor.ui.LoginFrame;
import javax.swing.*;

/**
 * MainApp - Application entry point
 * Starts the Smart Network Monitoring System
 */
public class MainApp {

    public static void main(String[] args) {
        // Set system properties
        System.setProperty("java.awt.headless", "false");

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
}
