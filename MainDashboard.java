package com.networkmonitor.ui;

import com.networkmonitor.model.User;
import com.networkmonitor.service.AlertService;
import com.networkmonitor.service.AuthService;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * MainDashboard - Main application window with tabbed interface and high-contrast UI
 */
public class MainDashboard extends JFrame {

    private User currentUser;
    private AuthService authService;
    private JTabbedPane tabbedPane;
    private JLabel alertBadgeLabel;
    private JLabel userInfoLabel;
    private Timer alertRefreshTimer;

    // UI Panels
    private DevicePanel devicePanel;
    private MonitoringPanel monitoringPanel;
    private SecurityPanel securityPanel;
    private FirewallPanel firewallPanel;
    private OptimizationPanel optimizationPanel;
    private AlertPanel alertPanel;
    private ReportPanel reportPanel;
    private UserManagementPanel userManagementPanel;

    public MainDashboard(User user) {
        this.currentUser = user;
        this.authService = AuthService.getInstance();
        initializeUI();
        setupAutoRefresh();
    }

    private void initializeUI() {
        setTitle("Smart Network Monitor — " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1380, 820);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UITheme.BG_CANVAS);

        // Header Bar
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Main Tabbed Container
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(UITheme.FONT_SUBHEADER);
        tabbedPane.setBackground(UITheme.CARD_BG);

        createPanels();
        addPanelsToTabs();

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Footer Bar
        JPanel footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleLogout();
            }
        });
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setBackground(UITheme.BG_DARK_HEADER);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        // Brand Title
        JLabel titleLabel = new JLabel("🌐 Smart Network Monitoring System");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_LIGHT);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Center Alert Indicator
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerPanel.setOpaque(false);

        alertBadgeLabel = new JLabel("🔔 Alerts: 0");
        alertBadgeLabel.setFont(UITheme.FONT_BODY_BOLD);
        alertBadgeLabel.setForeground(UITheme.WARNING_ORANGE);
        alertBadgeLabel.setOpaque(true);
        alertBadgeLabel.setBackground(new Color(30, 41, 59)); // Dark slate pill
        alertBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.WARNING_ORANGE, 1, true),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        centerPanel.add(alertBadgeLabel);
        headerPanel.add(centerPanel, BorderLayout.CENTER);

        // Right User Profile & Logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightPanel.setOpaque(false);

        userInfoLabel = new JLabel("👤 " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
        userInfoLabel.setFont(UITheme.FONT_BODY);
        userInfoLabel.setForeground(UITheme.TEXT_LIGHT);
        rightPanel.add(userInfoLabel);

        JButton logoutButton = new JButton("Sign Out");
        UITheme.styleDangerButton(logoutButton);
        logoutButton.addActionListener(e -> handleLogout());
        rightPanel.add(logoutButton);

        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new BorderLayout(10, 0));
        footerPanel.setBackground(UITheme.CARD_BG);
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_LIGHT),
            BorderFactory.createEmptyBorder(6, 20, 6, 20)
        ));

        JLabel statusLabel = new JLabel("● Database Connected (MySQL 8.0)");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.SUCCESS_GREEN);
        footerPanel.add(statusLabel, BorderLayout.WEST);

        JLabel timestampLabel = new JLabel("Session Active | " + currentUser.getUsername());
        timestampLabel.setFont(UITheme.FONT_SMALL);
        timestampLabel.setForeground(UITheme.TEXT_MUTED);
        footerPanel.add(timestampLabel, BorderLayout.EAST);

        return footerPanel;
    }

    private void createPanels() {
        devicePanel = new DevicePanel(currentUser);
        monitoringPanel = new MonitoringPanel(currentUser);
        securityPanel = new SecurityPanel(currentUser);
        firewallPanel = new FirewallPanel(currentUser);
        optimizationPanel = new OptimizationPanel(currentUser);
        alertPanel = new AlertPanel(currentUser);
        reportPanel = new ReportPanel(currentUser);
        userManagementPanel = new UserManagementPanel(currentUser);
    }

    private void addPanelsToTabs() {
        tabbedPane.addTab("📊 Monitoring", monitoringPanel);
        tabbedPane.addTab("🔔 Alerts", alertPanel);
        tabbedPane.addTab("📈 Reports", reportPanel);

        if (isRoleAllowed("OPERATOR")) {
            tabbedPane.addTab("📱 Devices", devicePanel);
            tabbedPane.addTab("🛡️ Security", securityPanel);
            tabbedPane.addTab("🔥 Firewall", firewallPanel);
            tabbedPane.addTab("⚡ Optimization", optimizationPanel);
        }

        if (isRoleAllowed("ADMIN")) {
            tabbedPane.addTab("👥 Users", userManagementPanel);
        }
    }

    private boolean isRoleAllowed(String requiredRole) {
        String userRole = currentUser.getRole();
        if ("ADMIN".equals(requiredRole)) {
            return "ADMIN".equals(userRole);
        } else if ("OPERATOR".equals(requiredRole)) {
            return "ADMIN".equals(userRole) || "OPERATOR".equals(userRole);
        }
        return true;
    }

    private void setupAutoRefresh() {
        alertRefreshTimer = new Timer(8000, e -> updateAlertBadge());
        alertRefreshTimer.start();
    }

    private void updateAlertBadge() {
        try {
            AlertService alertService = AlertService.getInstance();
            int count = alertService.getUnacknowledgedCount();

            SwingUtilities.invokeLater(() -> {
                if (count == 0) {
                    alertBadgeLabel.setText("🔔 Alerts: 0 Active");
                    alertBadgeLabel.setForeground(UITheme.TEXT_MUTED);
                    alertBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UITheme.BORDER_DARK, 1, true),
                        BorderFactory.createEmptyBorder(4, 12, 4, 12)
                    ));
                } else {
                    alertBadgeLabel.setText("🔔 Alerts: " + count + " Action Required");
                    alertBadgeLabel.setForeground(UITheme.DANGER_RED);
                    alertBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UITheme.DANGER_RED, 1, true),
                        BorderFactory.createEmptyBorder(4, 12, 4, 12)
                    ));
                }
            });
        } catch (Exception ignored) {}
    }

    private void handleLogout() {
        if (alertRefreshTimer != null) {
            alertRefreshTimer.stop();
        }
        authService.logout("127.0.0.1");
        dispose();
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
