package com.networkmonitor.ui;

import com.networkmonitor.dao.DeviceDAO;
import com.networkmonitor.model.BlockedIP;
import com.networkmonitor.model.Device;
import com.networkmonitor.model.SecurityEvent;
import com.networkmonitor.model.User;
import com.networkmonitor.service.SecurityService;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

/**
 * SecurityPanel - Threat detection feed and IP blacklisting interface
 */
public class SecurityPanel extends JPanel {

    private User currentUser;
    private SecurityService securityService;
    private DeviceDAO deviceDAO;

    private JTable eventsTable;
    private DefaultTableModel eventsTableModel;
    private JTable blockedIpTable;
    private DefaultTableModel blockedIpTableModel;

    private static final int EVENT_COL_ID = 0;
    private static final int EVENT_COL_DEVICE = 1;
    private static final int EVENT_COL_TYPE = 2;
    private static final int EVENT_COL_SEVERITY = 3;
    private static final int EVENT_COL_SOURCE_IP = 4;
    private static final int EVENT_COL_TIME = 5;

    private static final int IP_COL_ID = 0;
    private static final int IP_COL_ADDRESS = 1;
    private static final int IP_COL_REASON = 2;
    private static final int IP_COL_TIME = 3;

    public SecurityPanel(User currentUser) {
        this.currentUser = currentUser;
        this.securityService = SecurityService.getInstance();
        this.deviceDAO = new DeviceDAO();
        initializeUI();
        loadData();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Create Split Pane to hold both Events and Blocked IPs
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        splitPane.setDividerSize(10);
        splitPane.setBackground(UITheme.BG_CANVAS);

        // Top: Security Events
        splitPane.setTopComponent(createSecurityEventsPanel());

        // Bottom: Blocked IPs
        splitPane.setBottomComponent(createBlockedIpsPanel());

        // Initial split proportion
        splitPane.setResizeWeight(0.6);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createSecurityEventsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        UITheme.styleCard(panel);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("🛡️ Active Security Threats");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton resolveBtn = new JButton("✓ Resolve Selected");
        UITheme.styleSuccessButton(resolveBtn);
        resolveBtn.setEnabled(isOperatorOrAdmin());
        resolveBtn.addActionListener(e -> resolveSelectedEvent());
        btnPanel.add(resolveBtn);

        JButton refreshBtn = new JButton("🔄 Refresh");
        UITheme.styleNeutralButton(refreshBtn);
        refreshBtn.addActionListener(e -> loadData());
        btnPanel.add(refreshBtn);

        headerPanel.add(btnPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Table
        eventsTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        eventsTableModel.setColumnIdentifiers(new String[]{
            "ID", "Target Device", "Threat Type", "Severity", "Source IP", "Detected Time"
        });

        eventsTable = new JTable(eventsTableModel);
        UITheme.styleTable(eventsTable);
        eventsTable.getColumnModel().getColumn(EVENT_COL_SEVERITY).setCellRenderer(new SeverityCellRenderer());

        JScrollPane scrollPane = new JScrollPane(eventsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBlockedIpsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        UITheme.styleCard(panel);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("🚫 Blocked IP Addresses (Blacklist)");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton blockBtn = new JButton("➕ Manual Block");
        UITheme.styleDangerButton(blockBtn);
        blockBtn.setEnabled(isOperatorOrAdmin());
        blockBtn.addActionListener(e -> showBlockIpDialog());
        btnPanel.add(blockBtn);

        JButton unblockBtn = new JButton("🔓 Unblock Selected");
        UITheme.stylePrimaryButton(unblockBtn);
        unblockBtn.setEnabled(isOperatorOrAdmin());
        unblockBtn.addActionListener(e -> unblockSelectedIp());
        btnPanel.add(unblockBtn);

        headerPanel.add(btnPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Table
        blockedIpTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        blockedIpTableModel.setColumnIdentifiers(new String[]{
            "Block ID", "IP Address", "Reason", "Blocked At"
        });

        blockedIpTable = new JTable(blockedIpTableModel);
        UITheme.styleTable(blockedIpTable);

        JScrollPane scrollPane = new JScrollPane(blockedIpTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void loadData() {
        try {
            // Load Events
            eventsTableModel.setRowCount(0);
            List<SecurityEvent> events = securityService.getUnresolvedEvents();
            for (SecurityEvent ev : events) {
                Device dev = deviceDAO.getDeviceById(ev.getDeviceId());
                String devName = dev != null ? dev.getDeviceName() + " (" + dev.getIpAddress() + ")" : "Unknown";
                eventsTableModel.addRow(new Object[]{
                    ev.getEventId(), devName, ev.getEventType(), ev.getSeverity(),
                    ev.getSourceIp(), ev.getDetectedAt().toString()
                });
            }

            // Load Blocked IPs (Directly instantiated service methods logic here for UI brevity)
            blockedIpTableModel.setRowCount(0);
            com.networkmonitor.dao.BlockedIPDAO blockedDAO = new com.networkmonitor.dao.BlockedIPDAO();
            List<BlockedIP> blocked = blockedDAO.getActiveBlockedIPs();
            for (BlockedIP ip : blocked) {
                blockedIpTableModel.addRow(new Object[]{
                    ip.getBlockId(), ip.getIpAddress(), ip.getReason(), ip.getBlockedAt().toString()
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading security data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resolveSelectedEvent() {
        int row = eventsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an event to resolve.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int eventId = (Integer) eventsTableModel.getValueAt(row, EVENT_COL_ID);
        if (securityService.resolveEvent(eventId)) {
            loadData();
            JOptionPane.showMessageDialog(this, "Threat marked as resolved.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to resolve threat.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showBlockIpDialog() {
        String ip = JOptionPane.showInputDialog(this, "Enter IP Address to block:", "Manual Block", JOptionPane.WARNING_MESSAGE);
        if (ip != null && !ip.trim().isEmpty()) {
            if (com.networkmonitor.util.ValidationUtil.isValidIPv4(ip.trim())) {
                boolean success = securityService.blockIP(ip.trim(), "Manually Blocked by " + currentUser.getUsername(), currentUser.getUserId());
                if (success) {
                    loadData();
                    JOptionPane.showMessageDialog(this, "IP Blocked successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to block IP.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid IP Address format.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void unblockSelectedIp() {
        int row = blockedIpTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an IP to unblock.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String ip = (String) blockedIpTableModel.getValueAt(row, IP_COL_ADDRESS);
        if (securityService.unblockIP(ip)) {
            loadData();
            JOptionPane.showMessageDialog(this, "IP Unblocked.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to unblock IP.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isOperatorOrAdmin() {
        String role = currentUser.getRole();
        return "ADMIN".equals(role) || "OPERATOR".equals(role);
    }

    private static class SeverityCellRenderer extends JLabel implements TableCellRenderer {
        SeverityCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setFont(UITheme.FONT_BODY_BOLD);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String severity = (String) value;
            setText(severity);

            if ("CRITICAL".equals(severity)) {
                setBackground(new Color(254, 226, 226));
                setForeground(UITheme.DANGER_RED);
                setText("🔥 CRITICAL");
            } else if ("HIGH".equals(severity)) {
                setBackground(new Color(254, 243, 199));
                setForeground(UITheme.WARNING_ORANGE);
            } else if ("MEDIUM".equals(severity)) {
                setBackground(new Color(255, 237, 213));
                setForeground(new Color(234, 88, 12));
            } else {
                setBackground(new Color(241, 245, 249));
                setForeground(UITheme.TEXT_MUTED);
            }

            if (isSelected) {
                setBackground(UITheme.PRIMARY_BLUE);
                setForeground(UITheme.TEXT_LIGHT);
            }

            return this;
        }
    }
}
