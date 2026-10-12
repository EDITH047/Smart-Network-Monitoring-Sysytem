package com.networkmonitor.ui;

import com.networkmonitor.config.ConnectionPool;
import com.networkmonitor.model.User;
import com.networkmonitor.service.AlertService;
import com.networkmonitor.service.MonitoringService;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import java.awt.*;

/**
 * SettingsPanel - Application settings, alert thresholds, telemetry mode, and system diagnostics
 */
public class SettingsPanel extends JPanel {
    private User currentUser;
    private AlertService alertService;
    private MonitoringService monitoringService;

    private JSpinner bandwidthSpinner;
    private JSpinner latencySpinner;
    private JSpinner packetLossSpinner;
    private JCheckBox realDataToggle;
    private JLabel poolStatusLabel;

    public SettingsPanel(User currentUser) {
        this.currentUser = currentUser;
        this.alertService = AlertService.getInstance();
        this.monitoringService = MonitoringService.getInstance();
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("⚙️ System Settings & Preferences");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // Content Scroll Pane with Cards
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Card 1: Alert Thresholds
        JPanel alertCard = createCardPanel("🚨 Alert & Anomaly Detection Thresholds");
        JPanel alertGrid = new JPanel(new GridLayout(3, 2, 16, 12));
        alertGrid.setOpaque(false);
        alertGrid.setMaximumSize(new Dimension(600, 120));

        alertGrid.add(createLabel("Bandwidth Warning Threshold (%):"));
        bandwidthSpinner = new JSpinner(new SpinnerNumberModel(alertService.getBandwidthThreshold(), 10.0, 100.0, 5.0));
        alertGrid.add(bandwidthSpinner);

        alertGrid.add(createLabel("Latency Critical Threshold (ms):"));
        latencySpinner = new JSpinner(new SpinnerNumberModel(alertService.getLatencyThreshold(), 10.0, 2000.0, 25.0));
        alertGrid.add(latencySpinner);

        alertGrid.add(createLabel("Packet Loss Warning Threshold (%):"));
        packetLossSpinner = new JSpinner(new SpinnerNumberModel(alertService.getPacketLossThreshold(), 0.5, 50.0, 0.5));
        alertGrid.add(packetLossSpinner);

        alertCard.add(alertGrid);
        alertCard.add(Box.createVerticalStrut(14));

        JButton saveThresholdsBtn = new JButton("💾 Save Alert Thresholds");
        UITheme.stylePrimaryButton(saveThresholdsBtn);
        saveThresholdsBtn.addActionListener(e -> {
            try {
                double bw = ((Number) bandwidthSpinner.getValue()).doubleValue();
                double lat = ((Number) latencySpinner.getValue()).doubleValue();
                double pl = ((Number) packetLossSpinner.getValue()).doubleValue();

                alertService.setBandwidthThreshold(bw);
                alertService.setLatencyThreshold(lat);
                alertService.setPacketLossThreshold(pl);

                JOptionPane.showMessageDialog(this,
                    String.format("Alert thresholds updated successfully:\n• Bandwidth: %.1f%%\n• Latency: %.1f ms\n• Packet Loss: %.1f%%", bw, lat, pl),
                    "Thresholds Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to update thresholds: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        alertCard.add(saveThresholdsBtn);

        contentPanel.add(alertCard);
        contentPanel.add(Box.createVerticalStrut(16));

        // Card 2: Probing Mode
        JPanel probingCard = createCardPanel("📡 Network Telemetry & Data Source Mode");
        realDataToggle = new JCheckBox("Enable Real Hardware Telemetry (via Native OS Adapters)", monitoringService.isUsingRealData());
        realDataToggle.setFont(UITheme.FONT_BODY_BOLD);
        realDataToggle.setOpaque(false);
        realDataToggle.addActionListener(e -> {
            boolean isReal = realDataToggle.isSelected();
            monitoringService.setUseRealData(isReal);
            JOptionPane.showMessageDialog(this,
                "Telemetry mode switched to: " + (isReal ? "REAL OS NETWORK ADAPTERS" : "SIMULATED TELEMETRY"),
                "Telemetry Mode Changed", JOptionPane.INFORMATION_MESSAGE);
        });
        probingCard.add(realDataToggle);
        JLabel probingDesc = new JLabel("When disabled, system generates high-fidelity synthetic traffic to test security policies and alert conditions.");
        probingDesc.setFont(UITheme.FONT_SMALL);
        probingDesc.setForeground(UITheme.TEXT_MUTED);
        probingCard.add(Box.createVerticalStrut(4));
        probingCard.add(probingDesc);

        contentPanel.add(probingCard);
        contentPanel.add(Box.createVerticalStrut(16));

        // Card 3: Theme Settings
        JPanel themeCard = createCardPanel("🎨 UI Theme & Appearance");
        JPanel themeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        themeRow.setOpaque(false);

        JLabel currentThemeLabel = new JLabel(UITheme.isDarkMode() ? "🌙 Active: Dark Mode (Obsidian Slate)" : "☀️ Active: Light Mode (Modern Slate)");
        currentThemeLabel.setFont(UITheme.FONT_BODY_BOLD);
        currentThemeLabel.setForeground(UITheme.TEXT_PRIMARY);

        JButton toggleThemeBtn = new JButton("🌓 Switch Dark / Light Mode");
        UITheme.stylePrimaryButton(toggleThemeBtn);
        toggleThemeBtn.addActionListener(e -> {
            UITheme.toggleTheme(SwingUtilities.getWindowAncestor(this));
            currentThemeLabel.setText(UITheme.isDarkMode() ? "🌙 Active: Dark Mode (Obsidian Slate)" : "☀️ Active: Light Mode (Modern Slate)");
        });

        themeRow.add(toggleThemeBtn);
        themeRow.add(currentThemeLabel);
        themeCard.add(themeRow);

        JLabel themeDesc = new JLabel("High-contrast WCAG-compliant palette with distinct surface elevation, adaptive vector graphs, and persistent storage.");
        themeDesc.setFont(UITheme.FONT_SMALL);
        themeDesc.setForeground(UITheme.TEXT_MUTED);
        themeCard.add(Box.createVerticalStrut(6));
        themeCard.add(themeDesc);

        contentPanel.add(themeCard);
        contentPanel.add(Box.createVerticalStrut(16));

        // Card 4: System Information & Database Pool Status
        JPanel sysCard = createCardPanel("🖥️ System Diagnostics & Connection Pool");
        JPanel sysGrid = new JPanel(new GridLayout(4, 2, 16, 8));
        sysGrid.setOpaque(false);
        sysGrid.setMaximumSize(new Dimension(650, 120));

        sysGrid.add(createLabel("Active User:"));
        sysGrid.add(new JLabel((currentUser != null ? currentUser.getUsername() : "Admin") + " (" + (currentUser != null ? currentUser.getRole() : "ADMIN") + ")"));

        sysGrid.add(createLabel("Java Runtime:"));
        sysGrid.add(new JLabel(System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")"));

        sysGrid.add(createLabel("Operating System:"));
        sysGrid.add(new JLabel(System.getProperty("os.name") + " " + System.getProperty("os.arch")));

        sysGrid.add(createLabel("Connection Pool Status:"));
        try {
            ConnectionPool pool = com.networkmonitor.config.DatabaseConfig.getPool();
            poolStatusLabel = new JLabel(String.format("Available in Pool: %d | Actively in Use: %d",
                pool.getPoolSize(), pool.getUsedConnectionCount()));
        } catch (Exception ex) {
            poolStatusLabel = new JLabel("Connected via Dynamic Proxy");
        }
        sysGrid.add(poolStatusLabel);

        sysCard.add(sysGrid);
        contentPanel.add(sysCard);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createCardPanel(String title) {
        JPanel card = new JPanel();
        UITheme.styleCard(card);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1, true),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_SUBHEADER);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(titleLbl);
        card.add(Box.createVerticalStrut(12));

        return card;
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_BODY_BOLD);
        lbl.setForeground(UITheme.TEXT_MUTED);
        return lbl;
    }
}
