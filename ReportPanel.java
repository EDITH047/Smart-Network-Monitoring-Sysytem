package com.networkmonitor.ui;

import com.networkmonitor.dao.NetworkMetricDAO;
import com.networkmonitor.model.NetworkMetric;
import com.networkmonitor.model.User;
import com.networkmonitor.service.ExportService;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ReportPanel extends JPanel {
    private User currentUser;
    private NetworkMetricDAO metricDAO;

    public ReportPanel(User currentUser) {
        this.currentUser = currentUser;
        this.metricDAO = new NetworkMetricDAO();
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("📈 Reports & Analytics");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        UITheme.styleCard(centerPanel);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1, true),
            BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));

        JLabel infoLabel = new JLabel("Generate Data Reports");
        infoLabel.setFont(UITheme.FONT_TITLE);
        infoLabel.setForeground(UITheme.TEXT_PRIMARY);
        infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(infoLabel);
        centerPanel.add(Box.createVerticalStrut(20));

        // Form Fields
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 15));
        formPanel.setOpaque(false);
        formPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.setMaximumSize(new Dimension(500, 120));

        JLabel limitLabel = new JLabel("Data Limit:");
        limitLabel.setFont(UITheme.FONT_BODY_BOLD);
        JComboBox<String> limitCombo = new JComboBox<>(new String[]{"Last 1,000 Records", "Last 5,000 Records", "Last 10,000 Records"});
        
        JLabel formatLabel = new JLabel("Export Format:");
        formatLabel.setFont(UITheme.FONT_BODY_BOLD);
        JComboBox<String> formatCombo = new JComboBox<>(new String[]{"CSV File (*.csv)", "PDF Report (*.pdf) [Coming Soon]"});

        formPanel.add(limitLabel);
        formPanel.add(limitCombo);
        formPanel.add(formatLabel);
        formPanel.add(formatCombo);
        
        centerPanel.add(formPanel);
        centerPanel.add(Box.createVerticalStrut(30));

        JButton exportMetricsBtn = new JButton("📥 Export Network Metrics");
        UITheme.stylePrimaryButton(exportMetricsBtn);
        exportMetricsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportMetricsBtn.addActionListener(e -> {
            if (formatCombo.getSelectedIndex() == 1) {
                JOptionPane.showMessageDialog(this, "PDF Export is coming in a future update. Please select CSV.", "Information", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            try {
                int limit = 1000;
                if (limitCombo.getSelectedIndex() == 1) limit = 5000;
                if (limitCombo.getSelectedIndex() == 2) limit = 10000;
                
                List<NetworkMetric> metrics = metricDAO.getRecentMetrics(limit); 
                ExportService.exportMetricsToCSV(metrics, this);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error fetching data for export.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        centerPanel.add(exportMetricsBtn);
        centerPanel.add(Box.createVerticalGlue()); // Push everything up

        add(centerPanel, BorderLayout.CENTER);
    }
}
