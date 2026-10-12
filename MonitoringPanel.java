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
import java.text.SimpleDateFormat;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * MonitoringPanel — Enterprise Network Telemetry & Real-Time Performance Monitor.
 * Features:
 * - Dynamic View Modes: Balanced Split, Expanded Panoramic Charts, and Device Feed Focus
 * - Pure vector chart rendering (zero downsampling/compression)
 * - Anti-aliased telemetry curves with live throughput badges
 * - Sub-second background data collection with non-blocking EDT updates
 */
public class MonitoringPanel extends JPanel {

    private final User currentUser;
    private final MonitoringService monitoringService;
    private final DeviceDAO deviceDAO;
    private final java.util.concurrent.ScheduledExecutorService collectionExecutor =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
            r -> { Thread t = new Thread(r, "MetricCollector"); t.setDaemon(true); return t; });
    private final java.util.concurrent.ScheduledExecutorService persistenceExecutor =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
            r -> { Thread t = new Thread(r, "MetricPersister"); t.setDaemon(true); return t; });
    private Timer uiTimer;
    private boolean isRunning;

    // UI Components
    private JLabel systemHealthLabel;
    private JLabel onlineDevicesLabel;
    private JLabel liveBandwidthLabel;
    private JLabel avgBandwidthLabel;
    private JLabel networkSummaryLabel;
    private JTable metricsTable;
    private DefaultTableModel tableModel;
    private JLabel lastUpdateLabel;
    private JToggleButton realDataToggle;
    private JLabel dataSourceLabel;

    // Chart live badges
    private JLabel liveChartBwBadge;
    private JLabel liveChartLatBadge;

    // View Containers
    private JPanel centerContainer;
    private JSplitPane splitPane;
    private JPanel tablePanel;
    private JPanel chartsPanel;

    private JButton splitViewBtn;
    private JButton chartsFocusBtn;
    private JButton feedFocusBtn;

    // Charts
    private TimeSeries totalBandwidthSeries;
    private TimeSeries avgLatencySeries;
    private JFreeChart bwChart;
    private JFreeChart latChart;
    private int currentViewMode = 0;

    public MonitoringPanel(User currentUser) {
        this.currentUser = currentUser;
        this.monitoringService = MonitoringService.getInstance();
        this.deviceDAO = new DeviceDAO();
        this.isRunning = true;
        this.totalBandwidthSeries = new TimeSeries("Total Bandwidth (Mbps)");
        this.totalBandwidthSeries.setMaximumItemAge(120000); // 2 minutes history
        this.avgLatencySeries = new TimeSeries("Average Latency (ms)");
        this.avgLatencySeries.setMaximumItemAge(120000);
        initializeUI();
        setupAutoRefresh();
        UITheme.addThemeChangeListener(this::restyleCharts);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Top - Network Banner + Summary KPI Cards
        JPanel topPanel = new JPanel(new BorderLayout(0, 12));
        topPanel.setOpaque(false);

        JPanel networkBanner = createNetworkSummaryBanner();
        topPanel.add(networkBanner, BorderLayout.NORTH);

        JPanel summaryCards = createSummaryCards();
        topPanel.add(summaryCards, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // Center - Dynamic View Container
        tablePanel = createMetricsTable();
        chartsPanel = createChartsPanel();

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, chartsPanel);
        splitPane.setResizeWeight(0.48);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);

        tablePanel.setMinimumSize(new Dimension(300, 160));
        chartsPanel.setMinimumSize(new Dimension(300, 220));

        centerContainer = new JPanel(new BorderLayout());
        centerContainer.setOpaque(false);
        centerContainer.add(splitPane, BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);

        // Bottom - Status Bar & Controls
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createNetworkSummaryBanner() {
        JPanel banner = new JPanel(new BorderLayout(12, 0));
        banner.setBackground(new Color(15, 23, 42)); // Slate 900
        banner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        // Left: Data source indicator + mode toggle
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        dataSourceLabel = new JLabel("📡 LIVE STREAM");
        dataSourceLabel.setFont(UITheme.FONT_BODY_BOLD);
        dataSourceLabel.setForeground(UITheme.SUCCESS_GREEN);
        leftPanel.add(dataSourceLabel);

        realDataToggle = new JToggleButton("Real NIC: ON");
        realDataToggle.setSelected(true);
        realDataToggle.setFont(UITheme.FONT_SMALL);
        realDataToggle.setForeground(Color.WHITE);
        realDataToggle.setBackground(UITheme.SUCCESS_GREEN);
        realDataToggle.setFocusPainted(false);
        realDataToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        realDataToggle.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(16, 185, 129).darker(), 1, true),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        realDataToggle.addActionListener(e -> {
            boolean useReal = realDataToggle.isSelected();
            monitoringService.setUseRealData(useReal);
            if (useReal) {
                realDataToggle.setText("Real NIC: ON");
                realDataToggle.setBackground(UITheme.SUCCESS_GREEN);
                dataSourceLabel.setText("📡 LIVE STREAM");
                dataSourceLabel.setForeground(UITheme.SUCCESS_GREEN);
            } else {
                realDataToggle.setText("Simulated Stream");
                realDataToggle.setBackground(UITheme.WARNING_ORANGE);
                dataSourceLabel.setText("🎭 SIMULATED");
                dataSourceLabel.setForeground(UITheme.WARNING_ORANGE);
            }
            forceRefresh();
        });
        leftPanel.add(realDataToggle);
        banner.add(leftPanel, BorderLayout.WEST);

        // Center: Network summary subtitle
        networkSummaryLabel = new JLabel("Initializing network telemetry engine...");
        networkSummaryLabel.setFont(UITheme.FONT_SMALL);
        networkSummaryLabel.setForeground(new Color(148, 163, 184));
        networkSummaryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        banner.add(networkSummaryLabel, BorderLayout.CENTER);

        // Right: Segmented View Mode Switcher
        JPanel viewSwitchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        viewSwitchPanel.setOpaque(false);

        JLabel viewLbl = new JLabel("View Mode:");
        viewLbl.setFont(UITheme.FONT_SMALL);
        viewLbl.setForeground(new Color(148, 163, 184));
        viewSwitchPanel.add(viewLbl);

        splitViewBtn = createViewButton("🔀 Split", true);
        chartsFocusBtn = createViewButton("📈 Charts Focus", false);
        feedFocusBtn = createViewButton("📋 Device Feed", false);

        splitViewBtn.addActionListener(e -> setViewMode(0));
        chartsFocusBtn.addActionListener(e -> setViewMode(1));
        feedFocusBtn.addActionListener(e -> setViewMode(2));

        viewSwitchPanel.add(splitViewBtn);
        viewSwitchPanel.add(chartsFocusBtn);
        viewSwitchPanel.add(feedFocusBtn);
        banner.add(viewSwitchPanel, BorderLayout.EAST);

        return banner;
    }

    private JButton createViewButton(String title, boolean active) {
        JButton btn = new JButton(title);
        btn.setFont(UITheme.FONT_SMALL);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        styleViewButton(btn, active);
        return btn;
    }

    private void styleViewButton(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(UITheme.PRIMARY_BLUE);
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.PRIMARY_HOVER, 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
        } else {
            boolean dark = UITheme.isDarkMode();
            btn.setBackground(dark ? new Color(22, 31, 48) : new Color(30, 41, 59));
            btn.setForeground(dark ? new Color(148, 163, 184) : new Color(203, 213, 225));
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(dark ? new Color(38, 53, 74) : new Color(51, 65, 85), 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
        }
    }

    private void setViewMode(int mode) {
        this.currentViewMode = mode;
        styleViewButton(splitViewBtn, mode == 0);
        styleViewButton(chartsFocusBtn, mode == 1);
        styleViewButton(feedFocusBtn, mode == 2);

        centerContainer.removeAll();
        if (mode == 0) {
            splitPane.setTopComponent(tablePanel);
            splitPane.setBottomComponent(chartsPanel);
            splitPane.setResizeWeight(0.48);
            centerContainer.add(splitPane, BorderLayout.CENTER);
        } else if (mode == 1) {
            centerContainer.add(chartsPanel, BorderLayout.CENTER);
        } else {
            centerContainer.add(tablePanel, BorderLayout.CENTER);
        }
        centerContainer.revalidate();
        centerContainer.repaint();
    }

    private void restyleCharts() {
        if (bwChart != null) {
            Color bwLine = UITheme.isDarkMode() ? new Color(56, 189, 248) : new Color(2, 132, 199);
            styleChart(bwChart, bwLine);
        }
        if (latChart != null) {
            Color latLine = UITheme.SUCCESS_GREEN;
            styleChart(latChart, latLine);
        }
        if (splitViewBtn != null) {
            styleViewButton(splitViewBtn, currentViewMode == 0);
            styleViewButton(chartsFocusBtn, currentViewMode == 1);
            styleViewButton(feedFocusBtn, currentViewMode == 2);
        }
        repaint();
    }

    private JPanel createSummaryCards() {
        JPanel summaryPanel = new JPanel(new GridLayout(1, 4, 14, 0));
        summaryPanel.setOpaque(false);

        summaryPanel.add(createCard("🏥 Network Health", systemHealthLabel = new JLabel("Evaluating..."), "Active device availability score"));
        summaryPanel.add(createCard("📊 Active Devices", onlineDevicesLabel = new JLabel("— / —"), "Nodes responding to ICMP / TCP probe"));
        summaryPanel.add(createCard("⚡ Live Bandwidth", liveBandwidthLabel = new JLabel("— Mbps"), "Instantaneous stream throughput"));
        summaryPanel.add(createCard("📈 Historical Avg", avgBandwidthLabel = new JLabel("— Mbps"), "Aggregated multi-device baseline"));

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

        metricValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
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
        JPanel panel = new JPanel(new BorderLayout());
        UITheme.styleCard(panel);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel tableTitle = new JLabel("📈 Live Device Performance Feed");
        tableTitle.setFont(UITheme.FONT_HEADER);
        tableTitle.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(tableTitle, BorderLayout.WEST);

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableModel.setColumnIdentifiers(new String[]{
            "Device", "IP Address", "Status", "Live Bandwidth", "Latency", "Packet Loss", "Last Checked"
        });

        metricsTable = new JTable(tableModel);
        metricsTable.setAutoCreateRowSorter(true);
        UITheme.styleTable(metricsTable);

        headerPanel.add(UITheme.createSearchBar(metricsTable), BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        metricsTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        metricsTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        metricsTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        metricsTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        metricsTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        metricsTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        metricsTable.getColumnModel().getColumn(6).setPreferredWidth(110);

        metricsTable.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());
        metricsTable.getColumnModel().getColumn(4).setCellRenderer(new LatencyCellRenderer());
        metricsTable.getColumnModel().getColumn(5).setCellRenderer(new PacketLossCellRenderer());

        JScrollPane scrollPane = new JScrollPane(metricsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createChartsPanel() {
        JPanel container = new JPanel(new GridLayout(1, 2, 14, 0));
        container.setOpaque(false);

        // 1. Bandwidth Chart Card
        JPanel bwCard = new JPanel(new BorderLayout(0, 8));
        UITheme.styleCard(bwCard);

        JPanel bwHeader = new JPanel(new BorderLayout());
        bwHeader.setOpaque(false);
        JLabel bwTitle = new JLabel("⚡ Real-Time Throughput");
        bwTitle.setFont(UITheme.FONT_SUBHEADER);
        bwTitle.setForeground(UITheme.TEXT_PRIMARY);
        bwHeader.add(bwTitle, BorderLayout.WEST);

        liveChartBwBadge = new JLabel("● Live: 0.00 Mbps");
        liveChartBwBadge.setFont(UITheme.FONT_SMALL_BOLD);
        liveChartBwBadge.setForeground(new Color(2, 132, 199)); // Sky blue
        bwHeader.add(liveChartBwBadge, BorderLayout.EAST);
        bwCard.add(bwHeader, BorderLayout.NORTH);

        TimeSeriesCollection bwDataset = new TimeSeriesCollection(totalBandwidthSeries);
        this.bwChart = ChartFactory.createTimeSeriesChart(
            null, "Time", "Throughput (Mbps)", bwDataset, false, true, false);
        Color bwColor = UITheme.isDarkMode() ? new Color(56, 189, 248) : new Color(2, 132, 199);
        styleChart(this.bwChart, bwColor);

        ChartPanel bwChartPanel = new ChartPanel(this.bwChart);
        configureChartPanel(bwChartPanel);
        bwCard.add(bwChartPanel, BorderLayout.CENTER);
        container.add(bwCard);

        // 2. Latency Chart Card
        JPanel latCard = new JPanel(new BorderLayout(0, 8));
        UITheme.styleCard(latCard);

        JPanel latHeader = new JPanel(new BorderLayout());
        latHeader.setOpaque(false);
        JLabel latTitle = new JLabel("⏱️ Round-Trip Network Latency");
        latTitle.setFont(UITheme.FONT_SUBHEADER);
        latTitle.setForeground(UITheme.TEXT_PRIMARY);
        latHeader.add(latTitle, BorderLayout.WEST);

        liveChartLatBadge = new JLabel("● Avg: 0 ms");
        liveChartLatBadge.setFont(UITheme.FONT_SMALL_BOLD);
        liveChartLatBadge.setForeground(UITheme.SUCCESS_GREEN);
        latHeader.add(liveChartLatBadge, BorderLayout.EAST);
        latCard.add(latHeader, BorderLayout.NORTH);

        TimeSeriesCollection latDataset = new TimeSeriesCollection(avgLatencySeries);
        this.latChart = ChartFactory.createTimeSeriesChart(
            null, "Time", "Latency (ms)", latDataset, false, true, false);
        styleChart(this.latChart, UITheme.SUCCESS_GREEN);

        ChartPanel latChartPanel = new ChartPanel(this.latChart);
        configureChartPanel(latChartPanel);
        latCard.add(latChartPanel, BorderLayout.CENTER);
        container.add(latCard);

        return container;
    }

    private void configureChartPanel(ChartPanel cp) {
        cp.setOpaque(false);
        // Eliminate JFreeChart raster downsampling / compression
        cp.setMinimumDrawWidth(0);
        cp.setMinimumDrawHeight(0);
        cp.setMaximumDrawWidth(4000);
        cp.setMaximumDrawHeight(4000);
        cp.setPreferredSize(new Dimension(500, 260));
        cp.setDomainZoomable(true);
        cp.setRangeZoomable(true);
        cp.setMouseWheelEnabled(true);
    }

    private void styleChart(JFreeChart chart, Color lineColor) {
        chart.setAntiAlias(true);
        chart.setTextAntiAlias(true);
        chart.setBackgroundPaint(new Color(0, 0, 0, 0));

        boolean dark = UITheme.isDarkMode();
        Color plotBg = dark ? new Color(15, 23, 42) : new Color(248, 250, 252);
        Color gridColor = dark ? new Color(38, 53, 74) : new Color(226, 232, 240);
        Color outlineColor = dark ? new Color(62, 79, 102) : new Color(203, 213, 225);
        Color tickColor = dark ? new Color(148, 163, 184) : new Color(71, 85, 105);
        Color labelColor = dark ? new Color(248, 250, 252) : new Color(15, 23, 42);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(plotBg);
        plot.setDomainGridlinePaint(gridColor);
        plot.setRangeGridlinePaint(gridColor);
        plot.setOutlinePaint(outlineColor);

        // Time domain axis configuration: clean timestamps that never collide
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
        dateAxis.setAutoRange(true);
        dateAxis.setLowerMargin(0.02);
        dateAxis.setUpperMargin(0.02);
        dateAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
        dateAxis.setTickLabelPaint(tickColor);
        dateAxis.setLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        dateAxis.setLabelPaint(labelColor);

        // Value range axis configuration
        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
        rangeAxis.setTickLabelPaint(tickColor);
        rangeAxis.setLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        rangeAxis.setLabelPaint(labelColor);
        rangeAxis.setAutoRange(true);
        rangeAxis.setAutoRangeMinimumSize(5.0);

        // Smooth curve renderer
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, lineColor);
        renderer.setSeriesStroke(0, new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        plot.setRenderer(renderer);
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setOpaque(false);

        lastUpdateLabel = new JLabel("Auto-refresh active (500ms telemetry cycle)");
        lastUpdateLabel.setFont(UITheme.FONT_SMALL);
        lastUpdateLabel.setForeground(UITheme.TEXT_MUTED);
        bottomPanel.add(lastUpdateLabel, BorderLayout.WEST);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightButtons.setOpaque(false);

        JButton collectButton = new JButton("▶ Refresh Telemetry");
        UITheme.stylePrimaryButton(collectButton);
        collectButton.setToolTipText("Trigger an immediate metric collection cycle");
        collectButton.addActionListener(e -> {
            collectButton.setEnabled(false);
            collectButton.setText("Refreshing...");
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    monitoringService.collectMetrics();
                    return null;
                }
                @Override
                protected void done() {
                    collectButton.setEnabled(true);
                    collectButton.setText("▶ Refresh Telemetry");
                    refreshUI();
                }
            }.execute();
        });
        rightButtons.add(collectButton);

        bottomPanel.add(rightButtons, BorderLayout.EAST);
        return bottomPanel;
    }

    private void forceRefresh() {
        collectionExecutor.submit(() -> {
            try {
                monitoringService.collectMetrics();
                SwingUtilities.invokeLater(this::refreshUI);
            } catch (Exception ignored) {}
        });
    }

    private void setupAutoRefresh() {
        collectionExecutor.scheduleAtFixedRate(() -> {
            try {
                monitoringService.collectMetrics();
            } catch (Exception e) {
                System.err.println("[MonitoringPanel] Collection error: " + e.getMessage());
            }
        }, 0, 500, java.util.concurrent.TimeUnit.MILLISECONDS);

        persistenceExecutor.scheduleAtFixedRate(() -> {
            try {
                monitoringService.persistMetrics();
            } catch (Exception e) {
                System.err.println("[MonitoringPanel] Persistence error: " + e.getMessage());
            }
        }, 30, 30, java.util.concurrent.TimeUnit.SECONDS);

        uiTimer = new Timer(500, e -> {
            if (isRunning) refreshUI();
        });
        uiTimer.start();
    }

    private void refreshUI() {
        com.networkmonitor.service.DeviceCache cache = com.networkmonitor.service.DeviceCache.getInstance();
        List<NetworkMetric> metrics = monitoringService.getLatestMetrics();

        // Update summary cards
        double health = monitoringService.getSystemHealth();
        systemHealthLabel.setText(String.format("%.1f%%", health));
        systemHealthLabel.setForeground(health >= 80 ? UITheme.SUCCESS_GREEN : UITheme.DANGER_RED);

        int online = cache.getOnlineDeviceCount();
        int total = cache.getDeviceCount();
        onlineDevicesLabel.setText(online + " / " + total);
        onlineDevicesLabel.setForeground(online == total ? UITheme.SUCCESS_GREEN : UITheme.WARNING_ORANGE);

        double liveBw = monitoringService.getLiveBandwidth();
        if (liveBandwidthLabel != null) {
            liveBandwidthLabel.setText(String.format("%.2f Mbps", liveBw));
        }

        double avgBw = monitoringService.getAverageBandwidth();
        avgBandwidthLabel.setText(String.format("%.2f Mbps", avgBw));

        try {
            networkSummaryLabel.setText(monitoringService.getNetworkSummary());
        } catch (Exception e) {
            networkSummaryLabel.setText("Network summary stream active");
        }

        // Update table
        tableModel.setRowCount(0);
        double totalBw = 0;
        double totalLat = 0;
        int activeCount = 0;

        for (NetworkMetric metric : metrics) {
            Device device = cache.getDeviceById(metric.getDeviceId());
            if (device != null) {
                totalBw += metric.getBandwidthUsage();
                totalLat += metric.getLatencyMs();
                if (metric.getLatencyMs() > 0) activeCount++;

                tableModel.addRow(new Object[]{
                    device.getDeviceName(),
                    device.getIpAddress(),
                    device.getStatus(),
                    String.format("%.2f Mbps", metric.getBandwidthUsage()),
                    String.format("%.1f ms", metric.getLatencyMs()),
                    String.format("%.2f%%", metric.getPacketLossPct()),
                    formatTime()
                });
            }
        }

        Millisecond now = new Millisecond();
        totalBandwidthSeries.addOrUpdate(now, totalBw);
        double avgLat = activeCount > 0 ? totalLat / activeCount : 0;
        avgLatencySeries.addOrUpdate(now, avgLat);

        // Update live chart header badges
        if (liveChartBwBadge != null) {
            liveChartBwBadge.setText(String.format("● Live: %.2f Mbps", totalBw));
        }
        if (liveChartLatBadge != null) {
            liveChartLatBadge.setText(String.format("● Avg: %.1f ms", avgLat));
            liveChartLatBadge.setForeground(avgLat <= 50 ? UITheme.SUCCESS_GREEN : (avgLat <= 150 ? UITheme.WARNING_ORANGE : UITheme.DANGER_RED));
        }

        String modeTag = monitoringService.isUsingRealData() ? "[REAL]" : "[SIM]";
        lastUpdateLabel.setText(modeTag + " Stream Active • Latency: " + String.format("%.1f ms", avgLat) + " • Refresh: 500ms");
    }

    private String formatTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
    }

    public void cleanup() {
        isRunning = false;
        if (uiTimer != null) uiTimer.stop();
        collectionExecutor.shutdownNow();
        persistenceExecutor.shutdownNow();
    }

    private static class StatusCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        StatusCellRenderer() {
            setHorizontalAlignment(CENTER);
            setFont(UITheme.FONT_BODY_BOLD);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = value != null ? value.toString() : "";
            if ("ONLINE".equalsIgnoreCase(status)) {
                if (!isSelected) setForeground(UITheme.SUCCESS_GREEN);
                setText("🟢 ONLINE");
            } else {
                if (!isSelected) setForeground(UITheme.DANGER_RED);
                setText("🔴 OFFLINE");
            }
            return this;
        }
    }

    private static class LatencyCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        LatencyCellRenderer() {
            setHorizontalAlignment(RIGHT);
            setFont(UITheme.FONT_BODY);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected && value != null) {
                try {
                    String numStr = value.toString().replace("ms", "").trim();
                    double val = Double.parseDouble(numStr);
                    if (val > 150) setForeground(UITheme.DANGER_RED);
                    else if (val > 60) setForeground(UITheme.WARNING_ORANGE);
                    else setForeground(UITheme.SUCCESS_GREEN);
                } catch (Exception ignored) {}
            }
            return this;
        }
    }

    private static class PacketLossCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        PacketLossCellRenderer() {
            setHorizontalAlignment(RIGHT);
            setFont(UITheme.FONT_BODY);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected && value != null) {
                try {
                    String numStr = value.toString().replace("%", "").trim();
                    double val = Double.parseDouble(numStr);
                    if (val > 5.0) setForeground(UITheme.DANGER_RED);
                    else if (val > 1.0) setForeground(UITheme.WARNING_ORANGE);
                    else setForeground(UITheme.SUCCESS_GREEN);
                } catch (Exception ignored) {}
            }
            return this;
        }
    }
}
