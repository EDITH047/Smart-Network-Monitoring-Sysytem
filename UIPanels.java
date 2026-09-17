package com.networkmonitor.ui;

import com.networkmonitor.model.User;
import javax.swing.*;

// DevicePanel is now implemented in its own file: DevicePanel.java

// MonitoringPanel is now implemented in its own file: MonitoringPanel.java
// Features: Live metrics display, device status cards, real-time metrics, auto-refresh.

// SecurityPanel is now implemented in its own file: SecurityPanel.java

// FirewallPanel is now implemented in its own file: FirewallPanel.java

/**
 * OptimizationPanel - Bandwidth optimization
 * Shows optimization scores and recommendations
 */
class OptimizationPanel extends JPanel {
    private User currentUser;

    public OptimizationPanel(User currentUser) {
        this.currentUser = currentUser;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JLabel label = new JLabel("⚡ Network Optimization Panel - Coming Soon");
        label.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        add(label);
        add(Box.createVerticalStrut(20));
        add(new JLabel("Features: Optimization Scores, Bandwidth Recommendations"));
    }
}

/**
 * AlertPanel - Alert notifications
 * Displays unacknowledged alerts with filtering
 */
class AlertPanel extends JPanel {
    private User currentUser;

    public AlertPanel(User currentUser) {
        this.currentUser = currentUser;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JLabel label = new JLabel("🔔 Alerts & Notifications Panel - Coming Soon");
        label.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        add(label);
        add(Box.createVerticalStrut(20));
        add(new JLabel("Features: Alert Display, Acknowledge, Filter by Severity"));
    }
}

/**
 * ReportPanel - Report generation and export
 * Generate and export network reports to CSV
 */
class ReportPanel extends JPanel {
    private User currentUser;

    public ReportPanel(User currentUser) {
        this.currentUser = currentUser;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JLabel label = new JLabel("📈 Reports & Analytics Panel - Coming Soon");
        label.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        add(label);
        add(Box.createVerticalStrut(20));
        add(new JLabel("Features: Report Generation, CSV Export, Date Range Filtering"));
    }
}

/**
 * UserManagementPanel - User administration (Admin only)
 * Manage user accounts, roles, permissions
 */
class UserManagementPanel extends JPanel {
    private User currentUser;

    public UserManagementPanel(User currentUser) {
        this.currentUser = currentUser;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JLabel label = new JLabel("👥 User Management Panel - Coming Soon");
        label.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        add(label);
        add(Box.createVerticalStrut(20));
        add(new JLabel("Features: User CRUD, Role Assignment, Account Management"));
    }
}
