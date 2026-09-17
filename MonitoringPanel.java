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
import java.util.Timer;
import java.util.TimerTask;

/**
 * MonitoringPanel - Real-time Network Monitoring Dashboard with High-Contrast UI Theme
 */
public class MonitoringPanel extends JPanel {

    private User currentUser;
    private MonitoringService monitoringService;
    private DeviceDAO deviceDAO;
    private Timer refreshTimer;
    private boolean isRunning;

    // UI Components
    private JLabel systemHealthLabel;
    private JLabel onlineDevicesLabel;
    private JLabel avgBandwidthLabel;
    private JTable metricsTable;
    private DefaultTableModel tableModel;
    private JLabel lastUpdateLabel;

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

        // Top - Metric Cards
        JPanel topPanel = createSummaryCards();
        add(topPanel, BorderLayout.NORTH);

        // Center - Metrics Table
        JPanel centerPanel = createMetricsTable();
        add(centerPanel, BorderLayout.CENTER);

        // Bottom - Status & Refresh Control
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createSummaryCards() {
        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 14, 0));
        summaryPanel.setOpaque(false);

        // System Health Card
        JPanel healthCard = createCard(
            "🏥 Network Health",
            systemHealthLabel = new JLabel("Calculating..."),
            "Overall active device uptime status"
        );
        summaryPanel.add(healthCard);

        // Online Devices Card
        JPanel devicesCard = createCard(
            "📊 Active Devices",
            onlineDevicesLabel = new JLabel("0 / 0"),
            "Devices responding to network ping"
        );
        summaryPanel.add(devicesCard);

        // Average Bandwidth Card
        JPanel bandwidthCard = createCard(
            "⚡ Avg Bandwidth",
            avgBandwidthLabel = new JLabel("0 Mbps"),
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
        titleLabel.setForeground(UITheme.PRIMARY_BLUE);
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

        JButton refreshButton = new JButton("🔄 Refresh Stream");
        UITheme.stylePrimaryButton(refreshButton);
        refreshButton.addActionListener(e -> updateMetrics());
        bottomPanel.add(refreshButton, BorderLayout.EAST);

        return bottomPanel;
    }

    private void setupAutoRefresh() {
        refreshTimer = new Timer("MonitoringPanel-RefreshTimer", true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isRunning) {
                    SwingUtilities.invokeLater(MonitoringPanel.this::updateMetrics);
                }
            }
        }, 1000, 10000);
    }

    private void updateMetrics() {
        try {
            double systemHealth = monitoringService.getSystemHealth();
            int onlineDevices = deviceDAO.getOnlineDeviceCount();
            int totalDevices = deviceDAO.getDeviceCount();
            double avgBandwidth = monitoringService.getAverageBandwidth();

            systemHealthLabel.setText(String.format("%.1f%%", systemHealth));
            systemHealthLabel.setForeground(systemHealth >= 80 ? UITheme.SUCCESS_GREEN : UITheme.DANGER_RED);

            onlineDevicesLabel.setText(onlineDevices + " / " + totalDevices);
            onlineDevicesLabel.setForeground(onlineDevices == totalDevices ? UITheme.SUCCESS_GREEN : UITheme.WARNING_ORANGE);

            avgBandwidthLabel.setText(String.format("%.1f Mbps", avgBandwidth));

            tableModel.setRowCount(0);
            List<NetworkMetric> metrics = monitoringService.getLatestMetrics();

            for (NetworkMetric metric : metrics) {
                Device device = deviceDAO.getDeviceById(metric.getDeviceId());
                if (device != null) {
                    tableModel.addRow(new Object[]{
                        device.getDeviceName(),
                        device.getIpAddress(),
                        device.getStatus(),
                        String.format("%.1f Mbps", metric.getBandwidthUsage()),
                        String.format("%.1f ms", metric.getLatencyMs()),
                        String.format("%.2f%%", metric.getPacketLossPct()),
                        formatTime()
                    });
                }
            }

            lastUpdateLabel.setText("Last refreshed: " + formatTime() + " | Stream: Active (10s)");
        } catch (Exception e) {
            System.err.println("[MonitoringPanel] Metric update error: " + e.getMessage());
        }
    }

    private String formatTime() {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
    }

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
                    if (latency > 150) {
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
                    if (loss > 4) {
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

    public void cleanup() {
        isRunning = false;
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
    }
}
