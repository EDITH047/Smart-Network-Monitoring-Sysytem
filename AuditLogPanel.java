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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        
        tableModel = new DefaultTableModel(new String[]{"ID", "User", "Action", "Details", "IP Address", "Date"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        logTable = new JTable(tableModel);
        logTable.setAutoCreateRowSorter(true);
        UITheme.styleTable(logTable);
        
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);
        headerRight.add(UITheme.createSearchBar(logTable));
        
        JButton refreshBtn = new JButton("🔄 Refresh");
        UITheme.styleNeutralButton(refreshBtn);
        refreshBtn.addActionListener(e -> loadLogs());
        headerRight.add(refreshBtn);
        
        headerPanel.add(headerRight, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        UITheme.styleCard(centerPanel);
        
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
    
    private static class LogDisplayItem {
        final AuditLog log;
        final String username;
        LogDisplayItem(AuditLog log, String username) {
            this.log = log;
            this.username = username;
        }
    }

    private void loadLogs() {
        tableModel.setRowCount(0);
        new SwingWorker<List<LogDisplayItem>, Void>() {
            @Override
            protected List<LogDisplayItem> doInBackground() {
                List<AuditLog> logs = auditLogDAO.getAllLogs();
                Map<Integer, String> userCache = new HashMap<>();
                List<LogDisplayItem> items = new ArrayList<>(logs.size());
                for (AuditLog log : logs) {
                    int uid = log.getUserId();
                    String userStr = userCache.computeIfAbsent(uid, id -> {
                        User u = userDAO.findById(id);
                        return (u != null) ? u.getUsername() : "System/Unknown";
                    });
                    items.add(new LogDisplayItem(log, userStr));
                }
                return items;
            }
            @Override
            protected void done() {
                try {
                    List<LogDisplayItem> items = get();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    
                    if (items.isEmpty()) {
                        tableModel.addRow(new Object[]{"", "", "No logs found.", "", "", ""});
                    } else {
                        for (LogDisplayItem item : items) {
                            AuditLog log = item.log;
                            tableModel.addRow(new Object[]{
                                log.getLogId(),
                                item.username,
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
