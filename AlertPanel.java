package com.networkmonitor.ui;

import com.networkmonitor.dao.DeviceDAO;
import com.networkmonitor.model.Alert;
import com.networkmonitor.model.Device;
import com.networkmonitor.model.User;
import com.networkmonitor.service.AlertService;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.List;

public class AlertPanel extends JPanel {
    private User currentUser;
    private AlertService alertService;
    private DeviceDAO deviceDAO;
    private JTable alertTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> filterComboBox;
    private Timer refreshTimer;
    private boolean isRefreshing = false;

    public AlertPanel(User currentUser) {
        this.currentUser = currentUser;
        this.alertService = AlertService.getInstance();
        this.deviceDAO = new DeviceDAO();
        initializeUI();
        setupAutoRefresh();
        loadAlerts();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("🔔 Alert & Notification Center");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controlPanel.setOpaque(false);

        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setFont(UITheme.FONT_BODY);
        controlPanel.add(filterLabel);

        filterComboBox = new JComboBox<>(new String[]{"Unacknowledged", "All Alerts"});
        filterComboBox.setFont(UITheme.FONT_BODY);
        filterComboBox.addActionListener(e -> loadAlerts());
        controlPanel.add(filterComboBox);

        JButton refreshButton = new JButton("Refresh");
        UITheme.stylePrimaryButton(refreshButton);
        refreshButton.addActionListener(e -> loadAlerts());
        controlPanel.add(refreshButton);

        JButton ackButton = new JButton("Acknowledge Selected");
        UITheme.styleSuccessButton(ackButton);
        ackButton.addActionListener(e -> acknowledgeSelectedAlert());
        controlPanel.add(ackButton);

        headerPanel.add(controlPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Severity", "Device", "Alert Type", "Message", "Time", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        alertTable = new JTable(tableModel);
        UITheme.styleTable(alertTable);
        alertTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Custom cell renderer for Severity column
        alertTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected && value != null) {
                    String severity = value.toString();
                    if ("CRITICAL".equals(severity)) {
                        c.setForeground(UITheme.DANGER_RED);
                    } else if ("WARNING".equals(severity)) {
                        c.setForeground(UITheme.WARNING_ORANGE);
                    } else {
                        c.setForeground(UITheme.PRIMARY_BLUE);
                    }
                    c.setFont(UITheme.FONT_BODY_BOLD);
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(alertTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupAutoRefresh() {
        refreshTimer = new Timer(5000, e -> {
            loadAlerts();
        });
        refreshTimer.start();
    }

    private void loadAlerts() {
        if (isRefreshing) return;
        isRefreshing = true;

        new SwingWorker<List<Alert>, Void>() {
            @Override
            protected List<Alert> doInBackground() {
                String filter = (String) filterComboBox.getSelectedItem();
                if ("Unacknowledged".equals(filter)) {
                    return alertService.getUnacknowledgedAlerts();
                } else {
                    return alertService.getRecentAlerts(200); // limit to 200 for performance
                }
            }

            @Override
            protected void done() {
                try {
                    List<Alert> alerts = get();
                    updateTable(alerts);
                } catch (Exception e) {
                    System.err.println("[AlertPanel] Error loading alerts: " + e.getMessage());
                } finally {
                    isRefreshing = false;
                }
            }
        }.execute();
    }

    private void updateTable(List<Alert> alerts) {
        // Save selected row ID to restore selection after refresh
        int selectedId = -1;
        int selectedRow = alertTable.getSelectedRow();
        if (selectedRow >= 0) {
            selectedId = (int) tableModel.getValueAt(selectedRow, 0);
        }

        tableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        int newRowToSelect = -1;

        for (Alert alert : alerts) {
            Device device = deviceDAO.getDeviceById(alert.getDeviceId());
            String deviceStr = (device != null) ? device.getDeviceName() + " (" + device.getIpAddress() + ")" : "Unknown (ID: " + alert.getDeviceId() + ")";
            String statusStr = alert.isAcknowledged() ? "Acknowledged" : "Active";

            Object[] rowData = {
                    alert.getAlertId(),
                    alert.getSeverity(),
                    deviceStr,
                    alert.getAlertType(),
                    alert.getMessage(),
                    (alert.getCreatedAt() != null) ? sdf.format(alert.getCreatedAt()) : "N/A",
                    statusStr
            };
            
            tableModel.addRow(rowData);
            
            if (alert.getAlertId() == selectedId) {
                newRowToSelect = tableModel.getRowCount() - 1;
            }
        }

        if (newRowToSelect >= 0) {
            alertTable.setRowSelectionInterval(newRowToSelect, newRowToSelect);
        }
    }

    private void acknowledgeSelectedAlert() {
        int selectedRow = alertTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an alert to acknowledge.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int alertId = (int) tableModel.getValueAt(selectedRow, 0);
        String status = (String) tableModel.getValueAt(selectedRow, 6);

        if ("Acknowledged".equals(status)) {
            JOptionPane.showMessageDialog(this, "This alert is already acknowledged.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        boolean success = alertService.acknowledgeAlert(alertId, currentUser.getUserId());
        if (success) {
            JOptionPane.showMessageDialog(this, "Alert successfully acknowledged.", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadAlerts();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to acknowledge alert.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
