package com.networkmonitor.ui;

import com.networkmonitor.dao.DeviceDAO;
import com.networkmonitor.model.Device;
import com.networkmonitor.model.NetworkMetric;
import com.networkmonitor.model.User;
import com.networkmonitor.service.MonitoringService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * MonitoringPanel - Real-time network monitoring dashboard
 * Displays live device metrics, status, bandwidth usage, and system health
 */
public class MonitoringPanel extends JPanel implements ThemeManager.ThemeListener {

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

    private JPanel topPanel;
    private JPanel centerPanel;
    private JPanel bottomPanel;
    private JPanel healthCard;
    private JPanel devicesCard;
    private JPanel bandwidthCard;
    private JLabel systemHealthTitle;
    private JLabel devicesTitle;
    private JLabel bandwidthTitle;
    private JLabel systemHealthDesc;
    private JLabel devicesDesc;
    private JLabel bandwidthDesc;
    private JLabel tableTitle;
    private JPanel headerPanel;
    private JScrollPane scrollPane;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private JButton refreshButton;

    public MonitoringPanel(User currentUser) {
        this.currentUser = currentUser;
        this.monitoringService = MonitoringService.getInstance();
        this.deviceDAO = new DeviceDAO();
        this.isRunning = true;
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
        setBackground(ThemeManager.getBackgroundColor());
        
        topPanel.setBackground(ThemeManager.getBackgroundColor());
        centerPanel.setBackground(ThemeManager.getCardColor());
        bottomPanel.setBackground(ThemeManager.getBackgroundColor());
        
        healthCard.setBackground(ThemeManager.getCardColor());
        healthCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        devicesCard.setBackground(ThemeManager.getCardColor());
        devicesCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        bandwidthCard.setBackground(ThemeManager.getCardColor());
        bandwidthCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        systemHealthTitle.setForeground(ThemeManager.getPrimaryColor());
        devicesTitle.setForeground(ThemeManager.getPrimaryColor());
        bandwidthTitle.setForeground(ThemeManager.getPrimaryColor());
        
        systemHealthDesc.setForeground(ThemeManager.getTextMutedColor());
        devicesDesc.setForeground(ThemeManager.getTextMutedColor());
        bandwidthDesc.setForeground(ThemeManager.getTextMutedColor());
        
        centerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        tableTitle.setForeground(ThemeManager.getTextColor());
        headerPanel.setBackground(ThemeManager.getCardColor());
        
        metricsTable.getTableHeader().setBackground(ThemeManager.getPrimaryColor());
        metricsTable.getTableHeader().setForeground(Color.WHITE);
        metricsTable.setGridColor(ThemeManager.getBorderColor());
        metricsTable.setBackground(ThemeManager.getCardColor());
        metricsTable.setForeground(ThemeManager.getTextColor());
        
        scrollPane.setBackground(ThemeManager.getCardColor());
        scrollPane.getViewport().setBackground(ThemeManager.getCardColor());
        
        leftPanel.setBackground(ThemeManager.getBackgroundColor());
        rightPanel.setBackground(ThemeManager.getBackgroundColor());
        
        lastUpdateLabel.setForeground(ThemeManager.getTextMutedColor());
        
        refreshButton.setBackground(ThemeManager.getPrimaryColor());
        refreshButton.setForeground(Color.WHITE);
        
        // Trigger update to fix card metric colors
        updateMetrics();
        
        repaint();
    }

    /**
     * Initialize UI components
     */
    private void initializeUI() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top panel - Summary cards
        topPanel = createSummaryCards();
        add(topPanel, BorderLayout.NORTH);

        // Center panel - Metrics table
        centerPanel = createMetricsTable();
        add(centerPanel, BorderLayout.CENTER);

        // Bottom panel - Status and refresh info
        bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Create summary cards showing key metrics
     */
    private JPanel createSummaryCards() {
        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 15, 0));

        // System Health Card
        healthCard = new JPanel();
        healthCard.setLayout(new BoxLayout(healthCard, BoxLayout.Y_AXIS));
        systemHealthTitle = new JLabel("🏥 System Health");
        systemHealthTitle.setFont(new Font("Arial", Font.BOLD, 12));
        healthCard.add(systemHealthTitle);
        healthCard.add(Box.createVerticalStrut(8));
        systemHealthLabel = createMetricLabel("Calculating...");
        healthCard.add(systemHealthLabel);
        healthCard.add(Box.createVerticalStrut(8));
        systemHealthDesc = new JLabel("Overall network status");
        systemHealthDesc.setFont(new Font("Arial", Font.PLAIN, 9));
        healthCard.add(systemHealthDesc);
        summaryPanel.add(healthCard);

        // Online Devices Card
        devicesCard = new JPanel();
        devicesCard.setLayout(new BoxLayout(devicesCard, BoxLayout.Y_AXIS));
        devicesTitle = new JLabel("📊 Online Devices");
        devicesTitle.setFont(new Font("Arial", Font.BOLD, 12));
        devicesCard.add(devicesTitle);
        devicesCard.add(Box.createVerticalStrut(8));
        onlineDevicesLabel = createMetricLabel("0 / 0");
        devicesCard.add(onlineDevicesLabel);
        devicesCard.add(Box.createVerticalStrut(8));
        devicesDesc = new JLabel("Devices currently online");
        devicesDesc.setFont(new Font("Arial", Font.PLAIN, 9));
        devicesCard.add(devicesDesc);
        summaryPanel.add(devicesCard);

        // Average Bandwidth Card
        bandwidthCard = new JPanel();
        bandwidthCard.setLayout(new BoxLayout(bandwidthCard, BoxLayout.Y_AXIS));
        bandwidthTitle = new JLabel("⚡ Avg Bandwidth");
        bandwidthTitle.setFont(new Font("Arial", Font.BOLD, 12));
        bandwidthCard.add(bandwidthTitle);
        bandwidthCard.add(Box.createVerticalStrut(8));
        avgBandwidthLabel = createMetricLabel("0 Mbps");
        bandwidthCard.add(avgBandwidthLabel);
        bandwidthCard.add(Box.createVerticalStrut(8));
        bandwidthDesc = new JLabel("Average across all devices");
        bandwidthDesc.setFont(new Font("Arial", Font.PLAIN, 9));
        bandwidthCard.add(bandwidthDesc);
        summaryPanel.add(bandwidthCard);

        return summaryPanel;
    }

    // createCard method removed as it's no longer used

    /**
     * Create metric label
     */
    private JLabel createMetricLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * Create metrics table
     */
    private JPanel createMetricsTable() {
        JPanel tablePanel = new JPanel(new BorderLayout());

        // Table header
        tableTitle = new JLabel("📈 Real-Time Device Metrics");
        tableTitle.setFont(new Font("Arial", Font.BOLD, 12));
        headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(tableTitle);
        tablePanel.add(headerPanel, BorderLayout.NORTH);

        // Create table model
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableModel.setColumnIdentifiers(new String[]{
                "Device", "IP Address", "Status", "Bandwidth", "Latency", "Packet Loss", "Last Update"
        });

        metricsTable = new JTable(tableModel);
        metricsTable.setFont(new Font("Arial", Font.PLAIN, 10));
        metricsTable.setRowHeight(22);
        metricsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        metricsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 11));

        // Set column widths
        metricsTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        metricsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        metricsTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        metricsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        metricsTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        metricsTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        metricsTable.getColumnModel().getColumn(6).setPreferredWidth(120);

        // Set custom renderers
        metricsTable.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());
        metricsTable.getColumnModel().getColumn(3).setCellRenderer(new BandwidthCellRenderer());
        metricsTable.getColumnModel().getColumn(4).setCellRenderer(new LatencyCellRenderer());
        metricsTable.getColumnModel().getColumn(5).setCellRenderer(new PacketLossCellRenderer());

        // Scroll pane
        scrollPane = new JScrollPane(metricsTable);
        scrollPane.setBorder(null);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        return tablePanel;
    }

    /**
     * Create bottom panel with status and refresh info
     */
    private JPanel createBottomPanel() {
        JPanel pnl = new JPanel(new BorderLayout(10, 0));

        // Left: Last update info
        leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lastUpdateLabel = new JLabel("Last updated: Just now | Auto-refresh: Every 10 seconds");
        lastUpdateLabel.setFont(new Font("Arial", Font.PLAIN, 9));
        leftPanel.add(lastUpdateLabel);
        pnl.add(leftPanel, BorderLayout.WEST);

        // Right: Manual refresh button
        rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        refreshButton = new JButton("🔄 Refresh Now");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 10));
        refreshButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> updateMetrics());
        rightPanel.add(refreshButton);
        pnl.add(rightPanel, BorderLayout.EAST);

        return pnl;
    }

    /**
     * Setup auto-refresh timer (every 10 seconds)
     */
    private void setupAutoRefresh() {
        refreshTimer = new Timer("MonitoringPanel-Refresh", true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isRunning) {
                    SwingUtilities.invokeLater(MonitoringPanel.this::updateMetrics);
                }
            }
        }, 1000, 10000); // Start after 1 second, then every 10 seconds
    }

    /**
     * Update all metrics from database and UI
     */
    private void updateMetrics() {
        try {
            // Update summary cards
            double systemHealth = monitoringService.getSystemHealth();
            int onlineDevices = deviceDAO.getOnlineDeviceCount();
            int totalDevices = deviceDAO.getDeviceCount();
            double avgBandwidth = monitoringService.getAverageBandwidth();

            // Format system health with color
            String healthText = String.format("%.1f%%", systemHealth);
            Color healthColor = getHealthColor(systemHealth);
            systemHealthLabel.setText(healthText);
            systemHealthLabel.setForeground(healthColor);

            // Format online devices
            onlineDevicesLabel.setText(onlineDevices + " / " + totalDevices);
            onlineDevicesLabel.setForeground(onlineDevices == totalDevices ? ThemeManager.getSuccessColor() : ThemeManager.getErrorColor());

            // Format average bandwidth
            avgBandwidthLabel.setText(String.format("%.1f Mbps", avgBandwidth));
            avgBandwidthLabel.setForeground(ThemeManager.getPrimaryColor());

            // Update metrics table
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

            // Update last update label
            lastUpdateLabel.setText("Last updated: " + formatTime() + " | Auto-refresh: Every 10 seconds");

        } catch (Exception e) {
            System.err.println("[MonitoringPanel] Error updating metrics: " + e.getMessage());
        }
    }

    /**
     * Format current time
     */
    private String formatTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
        return sdf.format(new java.util.Date());
    }

    /**
     * Get color based on system health percentage
     */
    private Color getHealthColor(double health) {
        if (health >= 80) {
            return ThemeManager.getSuccessColor();
        } else if (health >= 60) {
            return ThemeManager.getWarningColor();
        } else {
            return ThemeManager.getErrorColor();
        }
    }

    /**
     * Custom renderer for status column
     */
    private static class StatusCellRenderer extends JLabel implements TableCellRenderer {
        StatusCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setFont(new Font("Arial", Font.BOLD, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            String status = (String) value;

            if ("ONLINE".equals(status)) {
                setBackground(ThemeManager.getSuccessBgColor());
                setForeground(ThemeManager.getSuccessColor());
                setText("🟢 " + status);
            } else if ("OFFLINE".equals(status)) {
                setBackground(ThemeManager.getErrorBgColor());
                setForeground(ThemeManager.getErrorColor());
                setText("🔴 " + status);
            } else {
                setBackground(ThemeManager.getWarningBgColor());
                setForeground(ThemeManager.getWarningColor());
                setText("🟡 " + status);
            }

            if (isSelected) {
                setBackground(ThemeManager.getPrimaryColor());
                setForeground(Color.WHITE);
            }

            return this;
        }
    }

    /**
     * Custom renderer for bandwidth column
     */
    private static class BandwidthCellRenderer extends JLabel implements TableCellRenderer {
        BandwidthCellRenderer() {
            setOpaque(true);
            setFont(new Font("Arial", Font.PLAIN, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            setText((String) value);

            if (isSelected) {
                setBackground(ThemeManager.getPrimaryColor());
                setForeground(Color.WHITE);
            } else {
                setBackground(ThemeManager.getCardColor());
                setForeground(ThemeManager.getTextColor());
            }

            return this;
        }
    }

    /**
     * Custom renderer for latency column
     */
    private static class LatencyCellRenderer extends JLabel implements TableCellRenderer {
        LatencyCellRenderer() {
            setOpaque(true);
            setFont(new Font("Arial", Font.PLAIN, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            String text = (String) value;
            setText(text);

            if (isSelected) {
                setBackground(ThemeManager.getPrimaryColor());
                setForeground(Color.WHITE);
            } else {
                setBackground(ThemeManager.getCardColor());
                // Color based on latency value
                try {
                    double latency = Double.parseDouble(text.replace(" ms", ""));
                    if (latency > 200) {
                        setForeground(ThemeManager.getErrorColor());
                    } else if (latency > 100) {
                        setForeground(ThemeManager.getWarningColor());
                    } else {
                        setForeground(ThemeManager.getSuccessColor());
                    }
                } catch (Exception e) {
                    setForeground(ThemeManager.getTextColor());
                }
            }

            return this;
        }
    }

    /**
     * Custom renderer for packet loss column
     */
    private static class PacketLossCellRenderer extends JLabel implements TableCellRenderer {
        PacketLossCellRenderer() {
            setOpaque(true);
            setFont(new Font("Arial", Font.PLAIN, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            String text = (String) value;
            setText(text);

            if (isSelected) {
                setBackground(ThemeManager.getPrimaryColor());
                setForeground(Color.WHITE);
            } else {
                setBackground(ThemeManager.getCardColor());
                // Color based on packet loss
                try {
                    double loss = Double.parseDouble(text.replace("%", ""));
                    if (loss > 5) {
                        setForeground(ThemeManager.getErrorColor());
                    } else if (loss > 2) {
                        setForeground(ThemeManager.getWarningColor());
                    } else {
                        setForeground(ThemeManager.getSuccessColor());
                    }
                } catch (Exception e) {
                    setForeground(ThemeManager.getTextColor());
                }
            }

            return this;
        }
    }

    /**
     * Cleanup when panel is closed
     */
    public void cleanup() {
        isRunning = false;
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
    }
    @Override
    public void removeNotify() {
        super.removeNotify();
        ThemeManager.removeThemeListener(this);
    }
}
