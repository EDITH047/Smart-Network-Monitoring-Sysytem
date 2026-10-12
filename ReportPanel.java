package com.networkmonitor.ui;

import com.networkmonitor.dao.NetworkMetricDAO;
import com.networkmonitor.model.NetworkMetric;
import com.networkmonitor.model.User;
import com.networkmonitor.service.ExportService;
import com.networkmonitor.service.ReportService;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Timestamp;
import java.util.List;

/**
 * ReportPanel - Multi-type report generation, live interactive preview, and CSV export
 */
public class ReportPanel extends JPanel {
    private User currentUser;
    private NetworkMetricDAO metricDAO;
    private ReportService reportService;

    private JComboBox<String> reportTypeCombo;
    private JComboBox<String> limitCombo;
    private JTable previewTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private String[][] currentReportData;

    public ReportPanel(User currentUser) {
        this.currentUser = currentUser;
        this.metricDAO = new NetworkMetricDAO();
        this.reportService = ReportService.getInstance();
        initializeUI();
        generateSelectedReport();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("📈 Reports & Analytics");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);
        headerPanel.add(statusLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Control Panel (Card)
        JPanel controlCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        UITheme.styleCard(controlCard);

        JLabel typeLbl = new JLabel("Report Type:");
        typeLbl.setFont(UITheme.FONT_BODY_BOLD);
        controlCard.add(typeLbl);

        String[] reportOptions = {
            "System Health & Device Overview",
            "Network Health Report (24h Averages)",
            "Security Incident Report (Recent)",
            "Bandwidth Utilization & Peaks",
            "Alert History Report",
            "Raw Network Metrics"
        };
        reportTypeCombo = new JComboBox<>(reportOptions);
        reportTypeCombo.setPreferredSize(new Dimension(270, 32));
        controlCard.add(reportTypeCombo);

        JLabel limitLbl = new JLabel("Data Limit:");
        limitLbl.setFont(UITheme.FONT_BODY_BOLD);
        controlCard.add(limitLbl);

        limitCombo = new JComboBox<>(new String[]{"1,000 Records", "5,000 Records", "10,000 Records"});
        limitCombo.setPreferredSize(new Dimension(130, 32));
        controlCard.add(limitCombo);

        JButton generateBtn = new JButton("🔄 Generate Report");
        UITheme.stylePrimaryButton(generateBtn);
        generateBtn.addActionListener(e -> generateSelectedReport());
        controlCard.add(generateBtn);

        JButton exportBtn = new JButton("📥 Export to CSV");
        UITheme.styleNeutralButton(exportBtn);
        exportBtn.addActionListener(e -> exportCurrentReport());
        controlCard.add(exportBtn);

        JButton exportHtmlBtn = new JButton("🌐 Export HTML / Print");
        UITheme.stylePrimaryButton(exportHtmlBtn);
        exportHtmlBtn.addActionListener(e -> exportCurrentReportToHTML());
        controlCard.add(exportHtmlBtn);

        // Preview Table in Center Panel
        tableModel = new DefaultTableModel();
        previewTable = new JTable(tableModel);
        UITheme.styleTable(previewTable);
        previewTable.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(previewTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1));
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(controlCard, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    private void generateSelectedReport() {
        int selectedIndex = reportTypeCombo.getSelectedIndex();
        statusLabel.setText("Generating report in background...");

        new SwingWorker<String[][], Void>() {
            @Override
            protected String[][] doInBackground() {
                Timestamp now = new Timestamp(System.currentTimeMillis());
                Timestamp yesterday = new Timestamp(System.currentTimeMillis() - 24L * 60 * 60 * 1000);

                switch (selectedIndex) {
                    case 0:
                        return reportService.generateDevicePerformanceSummary();
                    case 1:
                        return reportService.generateNetworkHealthReport(yesterday, now);
                    case 2:
                        return reportService.generateSecurityReport(yesterday, now);
                    case 3:
                        return reportService.generateBandwidthReport(yesterday, now);
                    case 4:
                        return reportService.generateAlertSummaryReport(yesterday, now);
                    case 5:
                    default:
                        int limit = 1000;
                        if (limitCombo.getSelectedIndex() == 1) limit = 5000;
                        if (limitCombo.getSelectedIndex() == 2) limit = 10000;
                        List<NetworkMetric> metrics = metricDAO.getRecentMetrics(limit);
                        String[][] data = new String[metrics.size() + 1][6];
                        data[0] = new String[]{"Metric ID", "Device ID", "Bandwidth (Mbps)", "Latency (ms)", "Packet Loss (%)", "Timestamp"};
                        for (int i = 0; i < metrics.size(); i++) {
                            NetworkMetric m = metrics.get(i);
                            data[i + 1] = new String[]{
                                String.valueOf(m.getMetricId()),
                                String.valueOf(m.getDeviceId()),
                                String.format("%.2f", m.getBandwidthUsage()),
                                String.format("%.2f", m.getLatencyMs()),
                                String.format("%.2f%%", m.getPacketLossPct()),
                                m.getRecordedAt() != null ? m.getRecordedAt().toString() : ""
                            };
                        }
                        return data;
                }
            }

            @Override
            protected void done() {
                try {
                    currentReportData = get();
                    if (currentReportData != null && currentReportData.length > 0) {
                        String[] headers = currentReportData[0];
                        tableModel.setDataVector(new Object[0][0], headers);

                        for (int i = 1; i < currentReportData.length; i++) {
                            tableModel.addRow(currentReportData[i]);
                        }
                        statusLabel.setText(String.format("Report generated: %d row(s) at %s",
                            currentReportData.length - 1, new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date())));
                    } else {
                        tableModel.setRowCount(0);
                        statusLabel.setText("No data returned for selected report criteria.");
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Report generation error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void exportCurrentReport() {
        if (currentReportData == null || currentReportData.length <= 1) {
            JOptionPane.showMessageDialog(this, "Please generate a report first before exporting.", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String reportName = ((String) reportTypeCombo.getSelectedItem())
            .toLowerCase().replaceAll("[^a-z0-9]", "_") + ".csv";
        ExportService.exportTableDataToCSV(currentReportData, reportName, this);
    }

    private void exportCurrentReportToHTML() {
        if (currentReportData == null || currentReportData.length <= 1) {
            JOptionPane.showMessageDialog(this, "Please generate a report first before exporting.", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String reportTitle = (String) reportTypeCombo.getSelectedItem();
        String defaultFileName = reportTitle.toLowerCase().replaceAll("[^a-z0-9]", "_") + ".html";
        String[] headers = currentReportData[0];
        String[][] rows = new String[currentReportData.length - 1][];
        System.arraycopy(currentReportData, 1, rows, 0, currentReportData.length - 1);

        ExportService.exportTableDataToHTML(reportTitle, headers, rows, defaultFileName, this);
    }
}
