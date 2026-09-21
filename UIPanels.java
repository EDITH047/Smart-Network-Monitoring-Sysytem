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
        setBackground(new java.awt.Color(241, 245, 249));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel label = new JLabel("⚡ Network Optimization Panel - Coming Soon");
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        label.setForeground(new java.awt.Color(15, 23, 42));
        add(label);
        add(Box.createVerticalStrut(20));
        JLabel desc = new JLabel("Features: Optimization Scores, Bandwidth Recommendations");
        desc.setForeground(new java.awt.Color(71, 85, 105));
        add(desc);
    }
}

// AlertPanel is now implemented in its own file: AlertPanel.java
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
        setBackground(new java.awt.Color(241, 245, 249));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel label = new JLabel("📈 Reports & Analytics Panel - Coming Soon");
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        label.setForeground(new java.awt.Color(15, 23, 42));
        add(label);
        add(Box.createVerticalStrut(20));
        JLabel desc = new JLabel("Features: Report Generation, CSV Export, Date Range Filtering");
        desc.setForeground(new java.awt.Color(71, 85, 105));
        add(desc);
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
        setBackground(new java.awt.Color(241, 245, 249));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel label = new JLabel("👥 User Management Panel - Coming Soon");
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        label.setForeground(new java.awt.Color(15, 23, 42));
        add(label);
        add(Box.createVerticalStrut(20));
        JLabel desc = new JLabel("Features: User CRUD, Role Assignment, Account Management");
        desc.setForeground(new java.awt.Color(71, 85, 105));
        add(desc);
    }
}
