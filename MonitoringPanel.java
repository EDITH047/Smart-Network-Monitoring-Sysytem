package com.networkmonitor.ui;

import com.networkmonitor.dao.DeviceDAO;
import com.networkmonitor.model.Device;
import com.networkmonitor.model.NetworkMetric;
import com.networkmonitor.model.User;
import com.networkmonitor.service.MonitoringService;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

/**
 * MonitoringPanel - Real-time Network Monitoring Dashboard with High-Contrast UI Theme
 *
 * Enhanced with:
 * - Real/Simulated data toggle button
 * - Network summary display showing real system stats
 * - Data source indicator
 * - All heavy I/O runs on SwingWorker background threads to avoid freezing the UI
 */
public class MonitoringPanel extends JPanel {

    private User currentUser;
    private MonitoringService monitoringService;
    private DeviceDAO deviceDAO;
    private Timer refreshTimer; // javax.swing.Timer — fires on EDT
    private boolean isRunning;
    private boolean isRefreshing = false;

    // UI Components
    private JLabel systemHealthLabel;
    private JLabel onlineDevicesLabel;
    private JLabel avgBandwidthLabel;
    private JLabel networkSummaryLabel;
    private JTable metricsTable;
    private DefaultTableModel tableModel;
    private JLabel lastUpdateLabel;
    private JToggleButton realDataToggle;
    private JLabel dataSourceLabel;

    public MonitoringPanel(User currentUser) {
        this.currentUser = currentUser;
        this.monitoringService = MonitoringService.getInstance();
        this.deviceDAO = new DeviceDAO();
        this.isRunning = true;
        initializeUI();
        setupAutoRefresh();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Top - Summary Cards + Network Info
        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setOpaque(false);

        // Network Summary Banner
        JPanel networkBanner = createNetworkSummaryBanner();
        topPanel.add(networkBanner, BorderLayout.NORTH);

        // Metric Cards
        JPanel summaryCards = createSummaryCards();
        topPanel.add(summaryCards, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // Center - Metrics Table
        JPanel centerPanel = createMetricsTable();
        add(centerPanel, BorderLayout.CENTER);

        // Bottom - Status & Refresh Control
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createNetworkSummaryBanner() {
        JPanel banner = new JPanel(new BorderLayout(12, 0));
        banner.setBackground(new Color(30, 41, 59)); // Dark slate
        banner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        // Left: Data source indicator + toggle
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        dataSourceLabel = new JLabel("📡 REAL DATA");
        dataSourceLabel.setFont(UITheme.FONT_BODY_BOLD);
        dataSourceLabel.setForeground(UITheme.SUCCESS_GREEN);
        leftPanel.add(dataSourceLabel);

        realDataToggle = new JToggleButton("Real Data: ON");
        realDataToggle.setSelected(true);
        realDataToggle.setFont(UITheme.FONT_SMALL);
        realDataToggle.setForeground(Color.WHITE);
        realDataToggle.setBackground(UITheme.SUCCESS_GREEN);
        realDataToggle.setFocusPainted(false);
        realDataToggle.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        realDataToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        realDataToggle.addActionListener(e -> {
            boolean useReal = realDataToggle.isSelected();
            monitoringService.setUseRealData(useReal);
            if (useReal) {
                realDataToggle.setText("Real Data: ON");
                realDataToggle.setBackground(UITheme.SUCCESS_GREEN);
                dataSourceLabel.setText("📡 REAL DATA");
                dataSourceLabel.setForeground(UITheme.SUCCESS_GREEN);
            } else {
                realDataToggle.setText("Simulated Data");
                realDataToggle.setBackground(UITheme.WARNING_ORANGE);
                dataSourceLabel.setText("🎭 SIMULATED");
                dataSourceLabel.setForeground(UITheme.WARNING_ORANGE);
            }
            triggerBackgroundRefresh(); // Refresh immediately on background thread
        });
        leftPanel.add(realDataToggle);

        banner.add(leftPanel, BorderLayout.WEST);

        // Center: Network summary text
        networkSummaryLabel = new JLabel("Initializing network monitor...");
        networkSummaryLabel.setFont(UITheme.FONT_SMALL);
        networkSummaryLabel.setForeground(new Color(148, 163, 184)); // Slate 400
        networkSummaryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        banner.add(networkSummaryLabel, BorderLayout.CENTER);

        // Right: placeholder — will be updated in background
        JLabel interfaceInfo = new JLabel("🔌 Loading...");
        interfaceInfo.setFont(UITheme.FONT_SMALL);
        interfaceInfo.setForeground(new Color(148, 163, 184));
        banner.add(interfaceInfo, BorderLayout.EAST);

        // Load interface count in background (avoid blocking EDT during construction)
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    int ifCount = monitoringService.getAvailableInterfaces().size();
                    return "🔌 " + ifCount + " interface(s)";
                } catch (Exception e) {
                    return "🔌 N/A";
                }
            }
            @Override
            protected void done() {
                try {
                    interfaceInfo.setText(get());
                } catch (Exception ignored) {}
            }
        }.execute();

        return banner;
    }

    private JPanel createSummaryCards() {
        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 14, 0));
        summaryPanel.setOpaque(false);

        // System Health Card
        JPanel healthCard = createCard(
            "🏥 Network Health",
            systemHealthLabel = new JLabel("Loading..."),
            "Overall active device uptime status"
        );
        summaryPanel.add(healthCard);

        // Online Devices Card
        JPanel devicesCard = createCard(
            "📊 Active Devices",
            onlineDevicesLabel = new JLabel("— / —"),
            "Devices responding to network ping"
        );
        summaryPanel.add(devicesCard);

        // Average Bandwidth Card
        JPanel bandwidthCard = createCard(
            "⚡ Avg Bandwidth",
            avgBandwidthLabel = new JLabel("— Mbps"),
            "Real-time bandwidth utilization"
        );
        summaryPanel.add(bandwidthCard);

        return summaryPanel;
    }

    private JPanel createCard(String title, JLabel metricValue, String description) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        UITheme.styleCard(card);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_SUBHEADER);
        titleLabel.setForeground(UITheme.CARD_TITLE);
        card.add(titleLabel);

        card.add(Box.createVerticalStrut(6));

        metricValue.setFont(new Font("Segoe UI", Font.BOLD, 26));
        metricValue.setForeground(UITheme.TEXT_PRIMARY);
        card.add(metricValue);

        card.add(Box.createVerticalStrut(6));

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(UITheme.FONT_SMALL);
        descLabel.setForeground(UITheme.TEXT_MUTED);
        card.add(descLabel);

        return card;
    }

    private JPanel createMetricsTable() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        UITheme.styleCard(tablePanel);

        JLabel tableTitle = new JLabel("📈 Live Device Performance Feed");
        tableTitle.setFont(UITheme.FONT_HEADER);
        tableTitle.setForeground(UITheme.TEXT_PRIMARY);

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        headerPanel.add(tableTitle);
        tablePanel.add(headerPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableModel.setColumnIdentifiers(new String[]{
            "Device", "IP Address", "Status", "Bandwidth", "Latency", "Packet Loss", "Last Checked"
        });

        metricsTable = new JTable(tableModel);
        UITheme.styleTable(metricsTable);

        metricsTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        metricsTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        metricsTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        metricsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        metricsTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        metricsTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        metricsTable.getColumnModel().getColumn(6).setPreferredWidth(110);

        metricsTable.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());
        metricsTable.getColumnModel().getColumn(4).setCellRenderer(new LatencyCellRenderer());
        metricsTable.getColumnModel().getColumn(5).setCellRenderer(new PacketLossCellRenderer());

        JScrollPane scrollPane = new JScrollPane(metricsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        return tablePanel;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setOpaque(false);

        lastUpdateLabel = new JLabel("Auto-refresh active (every 10s)");
        lastUpdateLabel.setFont(UITheme.FONT_SMALL);
        lastUpdateLabel.setForeground(UITheme.TEXT_MUTED);
        bottomPanel.add(lastUpdateLabel, BorderLayout.WEST);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightButtons.setOpaque(false);

        JButton collectButton = new JButton("▶ Collect Now");
        UITheme.stylePrimaryButton(collectButton);
        collectButton.setToolTipText("Trigger an immediate metric collection cycle");
        collectButton.addActionListener(e -> {
            collectButton.setEnabled(false);
            collectButton.setText("Collecting...");
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    monitoringService.collectMetrics();
                    return null;
                }
                @Override
                protected void done() {
                    triggerBackgroundRefresh();
                    collectButton.setEnabled(true);
                    collectButton.setText("▶ Collect Now");
                }
            }.execute();
        });
        rightButtons.add(collectButton);

        JButton refreshButton = new JButton("🔄 Refresh View");
        UITheme.stylePrimaryButton(refreshButton);
        refreshButton.addActionListener(e -> triggerBackgroundRefresh());
        rightButtons.add(refreshButton);

        bottomPanel.add(rightButtons, BorderLayout.EAST);

        return bottomPanel;
    }

    /**
     * Auto-refresh timer — uses javax.swing.Timer which fires on EDT.
     * The handler kicks off a SwingWorker so the EDT is never blocked.
     */
    private void setupAutoRefresh() {
        refreshTimer = new Timer(500, e -> {
            if (isRunning) {
                triggerBackgroundRefresh();
            }
        });
        refreshTimer.setInitialDelay(500); // small delay before first tick
        refreshTimer.start();
    }

    /**
     * Container class for all dashboard data loaded off-EDT
     */
    private static class DashboardData {
        double systemHealth;
        int onlineDevices;
        int totalDevices;
        double avgBandwidth;
        String networkSummary;
        boolean isRealData;
        Object[][] tableRows; // rows for the table model
    }

    /**
     * Kick off a SwingWorker to load all dashboard data in the background.
     * When done, the EDT updates the UI with the results.
     */
    private void triggerBackgroundRefresh() {
        if (isRefreshing) return;
        isRefreshing = true;
        
        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() {
                DashboardData data = new DashboardData();
                try {
                    // Collect metrics in the background so the live feed has data
                    monitoringService.collectMetrics();

                    data.systemHealth = monitoringService.getSystemHealth();
                    data.onlineDevices = deviceDAO.getOnlineDeviceCount();
                    data.totalDevices = deviceDAO.getDeviceCount();
                    data.avgBandwidth = monitoringService.getAverageBandwidth();
                    data.isRealData = monitoringService.isUsingRealData();

                    try {
                        data.networkSummary = monitoringService.getNetworkSummary();
                    } catch (Exception e) {
                        data.networkSummary = "Network summary unavailable";
                    }

                    // Use the metrics just collected (already in live cache)
                    List<NetworkMetric> metrics = monitoringService.getLatestMetrics();
                    data.tableRows = new Object[metrics.size()][];
                    for (int i = 0; i < metrics.size(); i++) {
                        NetworkMetric metric = metrics.get(i);
                        Device device = deviceDAO.getDeviceById(metric.getDeviceId());
                        if (device != null) {
                            data.tableRows[i] = new Object[]{
                                device.getDeviceName(),
                                device.getIpAddress(),
                                device.getStatus(),
                                String.format("%.2f Mbps", metric.getBandwidthUsage()),
                                String.format("%.1f ms", metric.getLatencyMs()),
                                String.format("%.2f%%", metric.getPacketLossPct()),
                                formatTime()
                            };
                        }
                    }

                } catch (Exception e) {
                    System.err.println("[MonitoringPanel] Background refresh error: " + e.getMessage());
                }
                return data;
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();
                    applyDataToUI(data);
                } catch (Exception e) {
                    System.err.println("[MonitoringPanel] UI update error: " + e.getMessage());
                } finally {
                    isRefreshing = false;
                }
            }
        }.execute();
    }

    /**
     * Apply pre-fetched data to the UI components. Runs on the EDT.
     */
    private void applyDataToUI(DashboardData data) {
        // Summary cards
        systemHealthLabel.setText(String.format("%.1f%%", data.systemHealth));
        systemHealthLabel.setForeground(data.systemHealth >= 80 ? UITheme.SUCCESS_GREEN : UITheme.DANGER_RED);

        onlineDevicesLabel.setText(data.onlineDevices + " / " + data.totalDevices);
        onlineDevicesLabel.setForeground(
            data.onlineDevices == data.totalDevices ? UITheme.SUCCESS_GREEN : UITheme.WARNING_ORANGE);

        avgBandwidthLabel.setText(String.format("%.2f Mbps", data.avgBandwidth));

        // Network summary
        networkSummaryLabel.setText(data.networkSummary != null ? data.networkSummary : "—");

        // Table
        tableModel.setRowCount(0);
        if (data.tableRows != null) {
            for (Object[] row : data.tableRows) {
                if (row != null) {
                    tableModel.addRow(row);
                }
            }
        }

        // Status bar
        String modeTag = data.isRealData ? "[REAL]" : "[SIM]";
        lastUpdateLabel.setText(modeTag + " Last refreshed: " + formatTime() + " | Stream: Active (rapid)");
    }

    private String formatTime() {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
    }

    // ========== Custom Cell Renderers ==========

    private static class StatusCellRenderer extends JLabel implements TableCellRenderer {
        StatusCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setFont(UITheme.FONT_BODY_BOLD);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            String status = (String) value;

            if ("ONLINE".equals(status)) {
                setBackground(new Color(220, 252, 231));
                setForeground(UITheme.SUCCESS_GREEN);
                setText("🟢 ONLINE");
            } else if ("OFFLINE".equals(status)) {
                setBackground(new Color(254, 226, 226));
                setForeground(UITheme.DANGER_RED);
                setText("🔴 OFFLINE");
            } else if ("CRITICAL".equals(status)) {
                setBackground(new Color(254, 202, 202));
                setForeground(UITheme.DANGER_RED);
                setText("🔴 CRITICAL");
            } else {
                setBackground(new Color(254, 243, 199));
                setForeground(UITheme.WARNING_ORANGE);
                setText("🟡 WARNING");
            }

            if (isSelected) {
                setBackground(UITheme.PRIMARY_BLUE);
                setForeground(UITheme.TEXT_LIGHT);
            }

            return this;
        }
    }

    private static class LatencyCellRenderer extends JLabel implements TableCellRenderer {
        LatencyCellRenderer() {
            setOpaque(true);
            setFont(UITheme.FONT_BODY);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            String text = (String) value;
            setText(text);

            if (isSelected) {
                setBackground(UITheme.PRIMARY_BLUE);
                setForeground(UITheme.TEXT_LIGHT);
            } else {
                setBackground(UITheme.CARD_BG);
                try {
                    double latency = Double.parseDouble(text.replace(" ms", ""));
                    if (latency >= 9999) {
                        setForeground(UITheme.DANGER_RED);
                        setText("⛔ N/A");
                    } else if (latency > 150) {
                        setForeground(UITheme.DANGER_RED);
                    } else if (latency > 80) {
                        setForeground(UITheme.WARNING_ORANGE);
                    } else {
                        setForeground(UITheme.SUCCESS_GREEN);
                    }
                } catch (Exception e) {
                    setForeground(UITheme.TEXT_PRIMARY);
                }
            }
            return this;
        }
    }

    private static class PacketLossCellRenderer extends JLabel implements TableCellRenderer {
        PacketLossCellRenderer() {
            setOpaque(true);
            setFont(UITheme.FONT_BODY);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            String text = (String) value;
            setText(text);

            if (isSelected) {
                setBackground(UITheme.PRIMARY_BLUE);
                setForeground(UITheme.TEXT_LIGHT);
            } else {
                setBackground(UITheme.CARD_BG);
                try {
                    double loss = Double.parseDouble(text.replace("%", ""));
                    if (loss >= 100) {
                        setForeground(UITheme.DANGER_RED);
                        setText("⛔ 100%");
                    } else if (loss > 4) {
                        setForeground(UITheme.DANGER_RED);
                    } else if (loss > 1.5) {
                        setForeground(UITheme.WARNING_ORANGE);
                    } else {
                        setForeground(UITheme.SUCCESS_GREEN);
                    }
                } catch (Exception e) {
                    setForeground(UITheme.TEXT_PRIMARY);
                }
            }
            return this;
        }
    }

    /**
     * Clean up resources when the panel is removed
     */
    public void cleanup() {
        isRunning = false;
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }
}
