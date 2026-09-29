package com.networkmonitor.ui;

import com.networkmonitor.model.OptimizationResult;
import com.networkmonitor.model.User;
import com.networkmonitor.service.OptimizationService;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class OptimizationPanel extends JPanel {

    private User currentUser;
    private OptimizationService optimizationService;
    private JTable resultsTable;
    private DefaultTableModel tableModel;

    public OptimizationPanel(User currentUser) {
        this.currentUser = currentUser;
        this.optimizationService = OptimizationService.getInstance();
        initializeUI();
        loadResults();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("⚡ Network Optimization");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Buttons Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton analyzeBtn = new JButton("Run Full Analysis");
        UITheme.stylePrimaryButton(analyzeBtn);
        analyzeBtn.addActionListener(e -> runAnalysis());

        JButton refreshBtn = new JButton("🔄 Refresh");
        UITheme.styleNeutralButton(refreshBtn);
        refreshBtn.addActionListener(e -> loadResults());

        btnPanel.add(analyzeBtn);
        btnPanel.add(refreshBtn);
        headerPanel.add(btnPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Table
        JPanel tablePanel = new JPanel(new BorderLayout());
        UITheme.styleCard(tablePanel);

        tableModel = new DefaultTableModel(new String[]{
            "Device ID", "Current B/W", "Recommended", "Score", "Suggestion", "Analyzed At"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setAutoCreateRowSorter(true);
        UITheme.styleTable(resultsTable);

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        add(tablePanel, BorderLayout.CENTER);
    }

    private void loadResults() {
        tableModel.setRowCount(0);
        List<OptimizationResult> results = optimizationService.getLatestResults();
        if (results != null) {
            for (OptimizationResult r : results) {
                tableModel.addRow(new Object[]{
                    r.getDeviceId(),
                    String.format("%.2f Mbps", r.getCurrentBandwidth()),
                    String.format("%.2f Mbps", r.getRecommendedBandwidth()),
                    r.getOptimizationScore() + "/100",
                    r.getSuggestion(),
                    r.getAnalyzedAt()
                });
            }
        }
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
                    JOptionPane.showMessageDialog(OptimizationPanel.this, "Analysis complete.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(OptimizationPanel.this, "Analysis failed.");
                }
            }
        }.execute();
    }
}
