package com.networkmonitor.ui;

import com.networkmonitor.dao.AuditLogDAO;
import com.networkmonitor.dao.UserDAO;
import com.networkmonitor.model.AuditLog;
import com.networkmonitor.model.User;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class AuditLogPanel extends JPanel {
    private User currentUser;
    private AuditLogDAO auditLogDAO;
    private UserDAO userDAO;
    private JTable logTable;
    private DefaultTableModel tableModel;

    public AuditLogPanel(User currentUser) {
        this.currentUser = currentUser;
        this.auditLogDAO = new AuditLogDAO();
        this.userDAO = new UserDAO();
        initializeUI();
        loadLogs();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("📋 Audit Log");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JButton refreshBtn = new JButton("🔄 Refresh");
        UITheme.styleNeutralButton(refreshBtn);
        refreshBtn.addActionListener(e -> loadLogs());
        headerPanel.add(refreshBtn, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        UITheme.styleCard(centerPanel);

        tableModel = new DefaultTableModel(new String[]{"ID", "User", "Action", "Details", "IP Address", "Date"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        logTable = new JTable(tableModel);
        logTable.setAutoCreateRowSorter(true);
        UITheme.styleTable(logTable);
        
        logTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        logTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(300);
        logTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        logTable.getColumnModel().getColumn(5).setPreferredWidth(150);

        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }
    
    private void loadLogs() {
        tableModel.setRowCount(0);
        new SwingWorker<List<AuditLog>, Void>() {
            @Override
            protected List<AuditLog> doInBackground() {
                return auditLogDAO.getAllLogs();
            }
            @Override
            protected void done() {
                try {
                    List<AuditLog> logs = get();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    
                    if (logs.isEmpty()) {
                        tableModel.addRow(new Object[]{"", "", "No logs found.", "", "", ""});
                    } else {
                        for (AuditLog log : logs) {
                            User u = userDAO.findById(log.getUserId());
                            String userStr = (u != null) ? u.getUsername() : "System/Unknown";
                            tableModel.addRow(new Object[]{
                                log.getLogId(),
                                userStr,
                                log.getAction(),
                                log.getDetails(),
                                log.getIpAddress(),
                                (log.getPerformedAt() != null) ? sdf.format(log.getPerformedAt()) : ""
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }
}
