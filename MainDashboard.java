package com.networkmonitor.ui;

import com.networkmonitor.model.User;
import com.networkmonitor.service.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * MainDashboard - Main application window with tabbed interface
 * Contains all panels (Device, Monitoring, Security, Firewall, Optimization, Alerts, Reports, Users)
 * Visibility controlled by user role
 */
public class MainDashboard extends JFrame implements ThemeManager.ThemeListener {

    private User currentUser;
    private AuthService authService;
    private JTabbedPane tabbedPane;
    private JLabel alertBadgeLabel;
    private JLabel userInfoLabel;
    private Timer alertRefreshTimer;

    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel centerPanel;
    private JPanel rightPanel;
    private JPanel footerPanel;
    private JLabel titleLabel;
    private JLabel statusLabel;
    private JLabel timestampLabel;
    private JButton logoutButton;
    private JButton themeToggleButton;

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
        ThemeManager.addThemeListener(this);
        applyTheme();
    }

    @Override
    public void onThemeChanged() {
        applyTheme();
    }

    private void applyTheme() {
        mainPanel.setBackground(ThemeManager.getBackgroundColor());
        headerPanel.setBackground(ThemeManager.getPrimaryColor());
        centerPanel.setBackground(ThemeManager.getPrimaryColor());
        rightPanel.setBackground(ThemeManager.getPrimaryColor());
        footerPanel.setBackground(ThemeManager.getBorderColor());
        
        tabbedPane.setBackground(ThemeManager.getCardColor());
        tabbedPane.setForeground(ThemeManager.getTextColor());
        
        titleLabel.setForeground(Color.WHITE);
        alertBadgeLabel.setForeground(Color.WHITE);
        userInfoLabel.setForeground(Color.WHITE);
        
        logoutButton.setBackground(ThemeManager.getErrorColor());
        logoutButton.setForeground(Color.WHITE);
        
        themeToggleButton.setBackground(ThemeManager.getPrimaryColor().darker());
        themeToggleButton.setForeground(Color.WHITE);
        themeToggleButton.setText(ThemeManager.isDarkMode() ? "☀️ Light" : "🌙 Dark");
        
        statusLabel.setForeground(ThemeManager.getSuccessColor());
        timestampLabel.setForeground(ThemeManager.getTextMutedColor());
        
        // Ensure panels repaint
        mainPanel.repaint();
    }

    /**
     * Initialize main UI components
     */
    private void initializeUI() {
        setTitle("Smart Network Monitoring System - " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 800);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Error setting look and feel: " + e.getMessage());
        }

        // Main panel with BorderLayout
        mainPanel = new JPanel(new BorderLayout());

        // Header panel
        headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Tabbed pane for different sections
        tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setFont(new Font("Arial", Font.PLAIN, 12));

        // Create and add panels based on user role
        createPanels();
        addPanelsToTabs();

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Footer panel
        footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Handle window close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleLogout();
            }
        });
    }

    /**
     * Create header panel with user info and logout button
     */
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel();
        header.setLayout(new BorderLayout(10, 10));
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Title
        titleLabel = new JLabel("Smart Network Monitoring System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        header.add(titleLabel, BorderLayout.WEST);

        // Center panel for alerts
        centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));

        // Alert badge
        alertBadgeLabel = new JLabel("🔔 Alerts: 0");
        alertBadgeLabel.setFont(new Font("Arial", Font.BOLD, 12));
        centerPanel.add(alertBadgeLabel);

        header.add(centerPanel, BorderLayout.CENTER);

        // Right panel with user info and logout
        rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));

        // Theme Toggle Button
        themeToggleButton = new JButton();
        themeToggleButton.setFont(new Font("Arial", Font.BOLD, 11));
        themeToggleButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        themeToggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        themeToggleButton.setFocusPainted(false);
        themeToggleButton.addActionListener(e -> ThemeManager.toggleTheme());
        rightPanel.add(themeToggleButton);

        userInfoLabel = new JLabel("👤 " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
        userInfoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        rightPanel.add(userInfoLabel);

        logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Arial", Font.BOLD, 11));
        logoutButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.addActionListener(e -> handleLogout());
        rightPanel.add(logoutButton);

        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    /**
     * Create footer panel with status info
     */
    private JPanel createFooterPanel() {
        JPanel footer = new JPanel();
        footer.setLayout(new BorderLayout(10, 10));
        footer.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        statusLabel = new JLabel("Status: Connected ✓");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        footer.add(statusLabel, BorderLayout.WEST);

        timestampLabel = new JLabel("Last updated: " + new java.util.Date());
        timestampLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        footer.add(timestampLabel, BorderLayout.EAST);

        return footer;
    }

    /**
     * Create all UI panels
     */
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

    /**
     * Add panels to tabs based on user role
     */
    private void addPanelsToTabs() {
        // Everyone can see these
        tabbedPane.addTab("📊 Monitoring", monitoringPanel);
        tabbedPane.addTab("🔔 Alerts", alertPanel);
        tabbedPane.addTab("📈 Reports", reportPanel);

        // OPERATOR and ADMIN can see these
        if (isRoleAllowed("OPERATOR")) {
            tabbedPane.addTab("📱 Devices", devicePanel);
            tabbedPane.addTab("🛡️ Security", securityPanel);
            tabbedPane.addTab("🔥 Firewall", firewallPanel);
            tabbedPane.addTab("⚡ Optimization", optimizationPanel);
        }

        // ADMIN only
        if (isRoleAllowed("ADMIN")) {
            tabbedPane.addTab("👥 Users", userManagementPanel);
        }
    }

    /**
     * Check if user's role can access feature
     */
    private boolean isRoleAllowed(String requiredRole) {
        String userRole = currentUser.getRole();

        if ("ADMIN".equals(requiredRole)) {
            return "ADMIN".equals(userRole);
        } else if ("OPERATOR".equals(requiredRole)) {
            return "ADMIN".equals(userRole) || "OPERATOR".equals(userRole);
        }

        return true; // VIEWER can see public tabs
    }

    /**
     * Setup auto-refresh timer for alerts
     */
    private void setupAutoRefresh() {
        alertRefreshTimer = new Timer(10000, e -> updateAlertBadge()); // Every 10 seconds
        alertRefreshTimer.start();
    }

    /**
     * Update alert badge with unacknowledged count
     */
    private void updateAlertBadge() {
        try {
            AlertService alertService = AlertService.getInstance();
            int unacknowledgedCount = alertService.getUnacknowledgedCount();

            String badgeText;
            if (unacknowledgedCount == 0) {
                badgeText = "🔔 Alerts: None";
            } else if (unacknowledgedCount == 1) {
                badgeText = "🔔 Alerts: 1 new";
            } else {
                badgeText = "🔔 Alerts: " + unacknowledgedCount + " new";
            }

            SwingUtilities.invokeLater(() -> alertBadgeLabel.setText(badgeText));

        } catch (Exception e) {
            System.err.println("[MainDashboard] Error updating alert badge: " + e.getMessage());
        }
    }

    /**
     * Handle logout
     */
    private void handleLogout() {
        // Stop refresh timer
        if (alertRefreshTimer != null) {
            alertRefreshTimer.stop();
        }

        ThemeManager.removeThemeListener(this);

        // Logout from service
        authService.logout("127.0.0.1");
        System.out.println("[MainDashboard] User logged out");

        // Return to login
        dispose();
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }

    /**
     * Get alert badge label (used by AlertPanel to update)
     */
    public JLabel getAlertBadgeLabel() {
        return alertBadgeLabel;
    }

    /**
     * Get tabbed pane (used by panels)
     */
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    /**
     * Get current user
     */
    public User getCurrentUser() {
        return currentUser;
    }
}
