package com.networkmonitor.ui;

import com.networkmonitor.model.User;
import javax.swing.*;

// DevicePanel is now implemented in its own file: DevicePanel.java

// MonitoringPanel is now implemented in its own file: MonitoringPanel.java
// Features: Live metrics display, device status cards, real-time charts, auto-refresh

abstract class PlaceholderPanel extends JPanel implements ThemeManager.ThemeListener {
    protected JLabel titleLabel;
    protected JLabel descLabel;

    public PlaceholderPanel(String title, String desc) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        titleLabel = new JLabel(title);
        titleLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        add(titleLabel);
        
        add(Box.createVerticalStrut(20));
        
        descLabel = new JLabel(desc);
        add(descLabel);

        ThemeManager.addThemeListener(this);
        applyTheme();
    }

    @Override
    public void onThemeChanged() {
        applyTheme();
    }

    private void applyTheme() {
        setBackground(ThemeManager.getBackgroundColor());
        titleLabel.setForeground(ThemeManager.getTextColor());
        descLabel.setForeground(ThemeManager.getTextMutedColor());
        repaint();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        ThemeManager.removeThemeListener(this);
    }
}

/**
 * SecurityPanel - Security threat viewing
 * Displays detected threats, security events, blocked IPs
 */
class SecurityPanel extends PlaceholderPanel {
    private User currentUser;

    public SecurityPanel(User currentUser) {
        super("🛡️ Security Threats Panel - Coming Soon", "Features: Threat Detection, Event Logging, IP Blocking");
        this.currentUser = currentUser;
    }
}

/**
 * FirewallPanel - Firewall rule management
 * Add, edit, delete firewall rules with priority ordering
 */
class FirewallPanel extends PlaceholderPanel {
    private User currentUser;

    public FirewallPanel(User currentUser) {
        super("🔥 Firewall Rules Panel - Coming Soon", "Features: Rule Management, Priority Ordering, Traffic Filtering");
        this.currentUser = currentUser;
    }
}

/**
 * OptimizationPanel - Bandwidth optimization
 * Shows optimization scores and recommendations
 */
class OptimizationPanel extends PlaceholderPanel {
    private User currentUser;

    public OptimizationPanel(User currentUser) {
        super("⚡ Network Optimization Panel - Coming Soon", "Features: Optimization Scores, Bandwidth Recommendations");
        this.currentUser = currentUser;
    }
}

/**
 * AlertPanel - Alert notifications
 * Displays unacknowledged alerts with filtering
 */
class AlertPanel extends PlaceholderPanel {
    private User currentUser;

    public AlertPanel(User currentUser) {
        super("🔔 Alerts & Notifications Panel - Coming Soon", "Features: Alert Display, Acknowledge, Filter by Severity");
        this.currentUser = currentUser;
    }
}

/**
 * ReportPanel - Report generation and export
 * Generate and export network reports to CSV
 */
class ReportPanel extends PlaceholderPanel {
    private User currentUser;

    public ReportPanel(User currentUser) {
        super("📈 Reports & Analytics Panel - Coming Soon", "Features: Report Generation, CSV Export, Date Range Filtering");
        this.currentUser = currentUser;
    }
}

/**
 * UserManagementPanel - User administration (Admin only)
 * Manage user accounts, roles, permissions
 */
class UserManagementPanel extends PlaceholderPanel {
    private User currentUser;

    public UserManagementPanel(User currentUser) {
        super("👥 User Management Panel - Coming Soon", "Features: User CRUD, Role Assignment, Account Management");
        this.currentUser = currentUser;
    }
}
