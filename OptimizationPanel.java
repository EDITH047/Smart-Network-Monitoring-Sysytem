package com.networkmonitor.ui;

import com.networkmonitor.model.Device;
import com.networkmonitor.model.OptimizationResult;
import com.networkmonitor.model.User;
import com.networkmonitor.service.DeviceCache;
import com.networkmonitor.service.OptimizationService;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * OptimizationPanel — Actionable bandwidth optimization and QoS rule enforcement center.
 * Features KPI telemetry cards, color-coded health badges, and 1-click QoS rule injection.
 */
public class OptimizationPanel extends JPanel {

    private final User currentUser;
    private final OptimizationService optimizationService;
    private final DeviceCache deviceCache;

    private JTable resultsTable;
    private DefaultTableModel tableModel;

    private JLabel avgScoreLabel;
    private JLabel peakConsumerLabel;
    private JLabel totalAnalyzedLabel;

    private JButton qosRateLimitBtn;
    private JButton firewallDropBtn;
    private JButton runDiagBtn;

    public OptimizationPanel(User currentUser) {
        this.currentUser = currentUser;
        this.optimizationService = OptimizationService.getInstance();
        this.deviceCache = DeviceCache.getInstance();

        initializeUI();
        loadResults();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Header Panel
        JPanel topContainer = new JPanel(new BorderLayout(12, 12));
        topContainer.setOpaque(false);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("⚡ Network Optimization & QoS Policies");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Buttons Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton analyzeBtn = new JButton("🚀 Run Full Analysis");
        UITheme.stylePrimaryButton(analyzeBtn);
        analyzeBtn.addActionListener(e -> runAnalysis());

        JButton refreshBtn = new JButton("🔄 Refresh");
        UITheme.styleNeutralButton(refreshBtn);
        refreshBtn.addActionListener(e -> loadResults());

        btnPanel.add(analyzeBtn);
        btnPanel.add(refreshBtn);

        tableModel = new DefaultTableModel(new String[]{
            "ID", "Device Name", "IP Address", "Current B/W", "Recommended", "Efficiency Score", "Recommendation", "Analyzed At"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setAutoCreateRowSorter(true);

        btnPanel.add(UITheme.createSearchBar(resultsTable), 0);
        headerPanel.add(btnPanel, BorderLayout.EAST);
        topContainer.add(headerPanel, BorderLayout.NORTH);

        // KPI Summary Cards
        JPanel kpiGrid = new JPanel(new GridLayout(1, 3, 14, 0));
        kpiGrid.setOpaque(false);

        kpiGrid.add(createKpiCard("🏆 Network Health Score", avgScoreLabel = new JLabel("Evaluating..."), "Overall bandwidth distribution index"));
        kpiGrid.add(createKpiCard("🚀 Peak Consumer", peakConsumerLabel = new JLabel("None"), "Heaviest bandwidth demand node"));
        kpiGrid.add(createKpiCard("💡 Analyzed Nodes", totalAnalyzedLabel = new JLabel("0 Nodes"), "Evaluated in last 24 hours"));

        topContainer.add(kpiGrid, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // Table Panel
        JPanel tablePanel = new JPanel(new BorderLayout(8, 8));
        UITheme.styleCard(tablePanel);
        UITheme.styleTable(resultsTable);

        resultsTable.getColumnModel().getColumn(0).setMaxWidth(50);
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        resultsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        resultsTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        resultsTable.getColumnModel().getColumn(5).setPreferredWidth(130);
        resultsTable.getColumnModel().getColumn(6).setPreferredWidth(260);

        resultsTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, val, sel, foc, row, col);
                lbl.setFont(UITheme.FONT_BODY_BOLD);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                String text = val != null ? val.toString() : "";
                if (!sel) {
                    if (text.contains("Optimal") || text.contains("🟢")) {
                        lbl.setForeground(UITheme.SUCCESS_GREEN);
                    } else if (text.contains("Moderate") || text.contains("🟡")) {
                        lbl.setForeground(UITheme.WARNING_ORANGE);
                    } else {
                        lbl.setForeground(UITheme.DANGER_RED);
                    }
                }
                return lbl;
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom Action Toolbar for 1-Click Enforcement
        JPanel bottomActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        bottomActionPanel.setOpaque(false);

        qosRateLimitBtn = new JButton("⚡ Enforce QoS Policy (Rate Limit)");
        UITheme.stylePrimaryButton(qosRateLimitBtn);
        qosRateLimitBtn.setEnabled(false);
        qosRateLimitBtn.addActionListener(e -> applySelectedQoSRule("RATE_LIMIT"));
        bottomActionPanel.add(qosRateLimitBtn);

        firewallDropBtn = new JButton("🛡️ Apply Firewall Isolation Rule");
        UITheme.styleDangerButton(firewallDropBtn);
        firewallDropBtn.setEnabled(false);
        firewallDropBtn.addActionListener(e -> applySelectedQoSRule("DROP"));
        bottomActionPanel.add(firewallDropBtn);

        runDiagBtn = new JButton("🩺 Diagnostics & Port Scan");
        UITheme.styleNeutralButton(runDiagBtn);
        runDiagBtn.setEnabled(false);
        runDiagBtn.addActionListener(e -> runDiagnosticsOnSelected());
        bottomActionPanel.add(runDiagBtn);

        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = resultsTable.getSelectedRow() != -1;
            boolean canEdit = isOperatorOrAdmin();
            qosRateLimitBtn.setEnabled(hasSelection && canEdit);
            firewallDropBtn.setEnabled(hasSelection && canEdit);
            runDiagBtn.setEnabled(hasSelection);
        });

        tablePanel.add(bottomActionPanel, BorderLayout.SOUTH);
        add(tablePanel, BorderLayout.CENTER);
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        UITheme.styleCard(card);
        card.setBorder(BorderFactory.createCompoundBorder(
            card.getBorder(),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_SMALL_BOLD);
        titleLbl.setForeground(UITheme.TEXT_MUTED);
        card.add(titleLbl, BorderLayout.NORTH);

        valueLabel.setFont(UITheme.FONT_STAT);
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);
        card.add(valueLabel, BorderLayout.CENTER);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(UITheme.FONT_SMALL);
        subLbl.setForeground(UITheme.TEXT_MUTED);
        card.add(subLbl, BorderLayout.SOUTH);

        return card;
    }

    private void loadResults() {
        tableModel.setRowCount(0);
        List<OptimizationResult> results = optimizationService.getLatestResults();
        double totalScore = 0;
        double maxBandwidth = 0;
        String peakDevName = "None";

        if (results != null && !results.isEmpty()) {
            for (OptimizationResult r : results) {
                Device dev = deviceCache.getDeviceById(r.getDeviceId());
                String devName = dev != null ? dev.getDeviceName() : "Device #" + r.getDeviceId();
                String ip = dev != null ? dev.getIpAddress() : "N/A";

                int score = r.getOptimizationScore();
                totalScore += score;
                String scoreBadge;
                if (score >= 80) scoreBadge = "🟢 " + score + "/100 (Optimal)";
                else if (score >= 50) scoreBadge = "🟡 " + score + "/100 (Moderate)";
                else scoreBadge = "🔴 " + score + "/100 (Bottleneck)";

                if (r.getCurrentBandwidth() > maxBandwidth) {
                    maxBandwidth = r.getCurrentBandwidth();
                    peakDevName = String.format("%s (%.1f Mbps)", devName, maxBandwidth);
                }

                tableModel.addRow(new Object[]{
                    r.getDeviceId(),
                    devName,
                    ip,
                    String.format("%.2f Mbps", r.getCurrentBandwidth()),
                    String.format("%.2f Mbps", r.getRecommendedBandwidth()),
                    scoreBadge,
                    r.getSuggestion(),
                    r.getAnalyzedAt()
                });
            }

            int avg = (int) (totalScore / results.size());
            avgScoreLabel.setText(avg + " / 100");
            avgScoreLabel.setForeground(avg >= 75 ? UITheme.SUCCESS_GREEN : (avg >= 50 ? UITheme.WARNING_ORANGE : UITheme.DANGER_RED));
            peakConsumerLabel.setText(peakDevName);
            totalAnalyzedLabel.setText(results.size() + " Devices Analyzed");
        } else {
            avgScoreLabel.setText("N/A");
            peakConsumerLabel.setText("None");
            totalAnalyzedLabel.setText("0 Devices Analyzed");
        }
    }

    private void applySelectedQoSRule(String policyType) {
        int selectedRow = resultsTable.getSelectedRow();
        if (selectedRow == -1) return;

        int modelRow = resultsTable.convertRowIndexToModel(selectedRow);
        int deviceId = (Integer) tableModel.getValueAt(modelRow, 0);
        String devName = (String) tableModel.getValueAt(modelRow, 1);
        String ip = (String) tableModel.getValueAt(modelRow, 2);

        String prompt = "RATE_LIMIT".equals(policyType)
                ? String.format("Apply QoS Rate-Limiting firewall rule to %s (%s)?\nThis injects an active policy into Firewall rules.", devName, ip)
                : String.format("Apply DROP firewall isolation rule to %s (%s)?\nThis blocks incoming/outgoing packets.", devName, ip);

        int confirm = JOptionPane.showConfirmDialog(this, prompt, "Confirm QoS Policy",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = optimizationService.applyQoSRule(deviceId, policyType, currentUser);
            if (success) {
                JOptionPane.showMessageDialog(this,
                        String.format("Successfully enforced %s policy for %s!\nRule is now active in Firewall.", policyType, devName),
                        "QoS Policy Enforced", JOptionPane.INFORMATION_MESSAGE);
                loadResults();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to apply QoS rule.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void runDiagnosticsOnSelected() {
        int selectedRow = resultsTable.getSelectedRow();
        if (selectedRow == -1) return;

        int modelRow = resultsTable.convertRowIndexToModel(selectedRow);
        int deviceId = (Integer) tableModel.getValueAt(modelRow, 0);
        Device dev = deviceCache.getDeviceById(deviceId);

        NetworkDiagnosticsDialog dialog = new NetworkDiagnosticsDialog(
                SwingUtilities.getWindowAncestor(this), currentUser, dev
        );
        dialog.setVisible(true);
    }

    private void runAnalysis() {
        new SwingWorker<List<OptimizationResult>, Void>() {
            @Override
            protected List<OptimizationResult> doInBackground() {
                return optimizationService.analyzeAll();
            }

            @Override
            protected void done() {
                try {
                    get();
                    loadResults();
                    JOptionPane.showMessageDialog(OptimizationPanel.this,
                            "Full network optimization analysis completed successfully!",
                            "Analysis Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(OptimizationPanel.this,
                            "Analysis failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private boolean isOperatorOrAdmin() {
        if (currentUser == null) return false;
        String role = currentUser.getRole();
        return "ADMIN".equals(role) || "OPERATOR".equals(role);
    }
}
