package com.networkmonitor.ui;

import com.networkmonitor.model.Alert;
import com.networkmonitor.model.User;
import com.networkmonitor.service.AlertService;
import com.networkmonitor.service.AuthService;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    private int lastAlertCount = 0;

    // UI Panels
    private DevicePanel devicePanel;
    private MonitoringPanel monitoringPanel;
    private SecurityPanel securityPanel;
    private FirewallPanel firewallPanel;
    private OptimizationPanel optimizationPanel;
    private AlertPanel alertPanel;
    private ReportPanel reportPanel;
    private UserManagementPanel userManagementPanel;
    private NetworkTopologyPanel topologyPanel;
    private SettingsPanel settingsPanel;
    private AuditLogPanel auditLogPanel;

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
        setMinimumSize(new Dimension(1150, 720));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UITheme.BG_CANVAS);

        // Header Bar
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Main Tabbed Container
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        UITheme.styleTabbedPane(tabbedPane);

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

        alertBadgeLabel = new JLabel("🔔 Alerts: 0 Active");
        alertBadgeLabel.setFont(UITheme.FONT_BODY_BOLD);
        alertBadgeLabel.setForeground(new Color(203, 213, 225));
        alertBadgeLabel.setOpaque(true);
        alertBadgeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        alertBadgeLabel.setToolTipText("Click to open Quick Notification Center");
        alertBadgeLabel.setBackground(new Color(30, 41, 59)); // Dark slate pill
        alertBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        alertBadgeLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showQuickAlertDrawer(alertBadgeLabel);
            }
        });
        centerPanel.add(alertBadgeLabel);
        headerPanel.add(centerPanel, BorderLayout.CENTER);

        // Right User Profile & Logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightPanel.setOpaque(false);

        userInfoLabel = new JLabel("👤 " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
        userInfoLabel.setFont(UITheme.FONT_BODY);
        userInfoLabel.setForeground(UITheme.TEXT_LIGHT);
        rightPanel.add(userInfoLabel);

        JButton themeBtn = new JButton(UITheme.isDarkMode() ? "☀️ Light" : "🌙 Dark");
        themeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        themeBtn.setToolTipText("Toggle Dark / Light Theme");
        themeBtn.setFocusPainted(false);
        themeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        themeBtn.setBackground(new Color(30, 41, 59));
        themeBtn.setForeground(new Color(203, 213, 225));
        themeBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        themeBtn.addActionListener(e -> {
            UITheme.toggleTheme(this);
            themeBtn.setText(UITheme.isDarkMode() ? "☀️ Light" : "🌙 Dark");
        });
        rightPanel.add(themeBtn);

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
        topologyPanel = new NetworkTopologyPanel(currentUser);
        settingsPanel = new SettingsPanel(currentUser);
        auditLogPanel = new AuditLogPanel(currentUser);
    }

    private void addPanelsToTabs() {
        tabbedPane.addTab("📊 Monitoring", monitoringPanel);
        tabbedPane.addTab("🗺️ Topology", topologyPanel);
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
            tabbedPane.addTab("📋 Audit Log", auditLogPanel);
        }
        
        tabbedPane.addTab("⚙️ Settings", settingsPanel);
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
        CompletableFuture.supplyAsync(() -> {
            try {
                return AlertService.getInstance().getUnacknowledgedCount();
            } catch (Exception e) {
                return -1;
            }
        }).thenAccept(count -> {
            if (count < 0) return;
            SwingUtilities.invokeLater(() -> {
                if (count > lastAlertCount && lastAlertCount >= 0 && count > 0) {
                    // Audible chime for new alerts
                    try { Toolkit.getDefaultToolkit().beep(); } catch (Exception ignored) {}
                }
                lastAlertCount = count;

                if (count == 0) {
                    alertBadgeLabel.setText("🔔 Alerts: 0 Active");
                    alertBadgeLabel.setBackground(new Color(30, 41, 59));
                    alertBadgeLabel.setForeground(new Color(203, 213, 225));
                    alertBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
                        BorderFactory.createEmptyBorder(4, 12, 4, 12)
                    ));
                } else {
                    alertBadgeLabel.setText("🔔 " + count + " Unresolved Alerts (Click for Center)");
                    alertBadgeLabel.setBackground(new Color(127, 29, 29));
                    alertBadgeLabel.setForeground(new Color(254, 202, 202));
                    alertBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(239, 68, 68), 1, true),
                        BorderFactory.createEmptyBorder(4, 12, 4, 12)
                    ));
                }
            });
        });
    }

    private void showQuickAlertDrawer(Component invoker) {
        JPopupMenu drawer = new JPopupMenu();
        drawer.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1));

        JMenuItem header = new JMenuItem("🔔 Notification Center & Quick Triage");
        header.setFont(UITheme.FONT_BODY_BOLD);
        header.setEnabled(false);
        drawer.add(header);
        drawer.addSeparator();

        List<Alert> unack = AlertService.getInstance().getUnacknowledgedAlerts();
        if (unack == null || unack.isEmpty()) {
            JMenuItem empty = new JMenuItem("✓ All alerts acknowledged. Network clear!");
            empty.setFont(UITheme.FONT_SMALL);
            empty.setEnabled(false);
            drawer.add(empty);
        } else {
            int displayCount = Math.min(5, unack.size());
            for (int i = 0; i < displayCount; i++) {
                Alert a = unack.get(i);
                String prefix = "CRITICAL".equalsIgnoreCase(a.getSeverity()) ? "🔴 " : ("HIGH".equalsIgnoreCase(a.getSeverity()) ? "🟠 " : "🟡 ");
                JMenuItem item = new JMenuItem(prefix + a.getMessage());
                item.setFont(UITheme.FONT_SMALL);
                drawer.add(item);
            }
            if (unack.size() > 5) {
                JMenuItem more = new JMenuItem(String.format("... and %d more alerts", unack.size() - 5));
                more.setFont(UITheme.FONT_SMALL);
                more.setEnabled(false);
                drawer.add(more);
            }

            drawer.addSeparator();
            JMenuItem ackAll = new JMenuItem("✓ Acknowledge All Alerts (" + unack.size() + ")");
            ackAll.setFont(UITheme.FONT_SMALL_BOLD);
            ackAll.setForeground(UITheme.SUCCESS_GREEN);
            ackAll.addActionListener(e -> {
                for (Alert a : unack) {
                    AlertService.getInstance().acknowledgeAlert(a.getAlertId(), currentUser.getUserId());
                }
                updateAlertBadge();
                if (alertPanel != null) {
                    tabbedPane.setSelectedComponent(alertPanel);
                }
            });
            drawer.add(ackAll);
        }

        drawer.addSeparator();
        JMenuItem viewTab = new JMenuItem("📑 Open Full Alerts Dashboard");
        viewTab.setFont(UITheme.FONT_SMALL_BOLD);
        viewTab.addActionListener(e -> tabbedPane.setSelectedComponent(alertPanel));
        drawer.add(viewTab);

        drawer.show(invoker, 0, invoker.getHeight() + 4);
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
