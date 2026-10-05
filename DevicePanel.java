package com.networkmonitor.ui;

import com.networkmonitor.dao.DeviceDAO;
import com.networkmonitor.model.Device;
import com.networkmonitor.model.User;
import com.networkmonitor.util.UITheme;
import com.networkmonitor.util.ValidationUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

/**
 * DevicePanel - High-contrast, modern UI for Network Device Management
 */
public class DevicePanel extends JPanel {

    private User currentUser;
    private DeviceDAO deviceDAO;
    private JTable deviceTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private JLabel statusLabel;

    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_IP = 2;
    private static final int COL_MAC = 3;
    private static final int COL_TYPE = 4;
    private static final int COL_LOCATION = 5;
    private static final int COL_STATUS = 6;

    public DevicePanel(User currentUser) {
        this.currentUser = currentUser;
        this.deviceDAO = new DeviceDAO();
        initializeUI();
        loadDevices();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Top Control Header
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // Center Table Container
        JPanel centerPanel = createTablePanel();
        add(centerPanel, BorderLayout.CENTER);

        // Bottom Status Bar
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("📱 Network Device Registry");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        topPanel.add(titleLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        addButton = new JButton("➕ Add Device");
        UITheme.styleSuccessButton(addButton);
        addButton.setEnabled(isOperatorOrAdmin());
        addButton.addActionListener(e -> showAddDeviceDialog());
        buttonPanel.add(addButton);

        editButton = new JButton("✏️ Edit");
        UITheme.stylePrimaryButton(editButton);
        editButton.setEnabled(isOperatorOrAdmin());
        editButton.addActionListener(e -> showEditDeviceDialog());
        buttonPanel.add(editButton);

        deleteButton = new JButton("🗑️ Delete");
        UITheme.styleDangerButton(deleteButton);
        deleteButton.setEnabled(isOperatorOrAdmin());
        deleteButton.addActionListener(e -> showDeleteConfirmation());
        buttonPanel.add(deleteButton);

        JButton wifiButton = new JButton("📡 Scan WiFi");
        UITheme.stylePrimaryButton(wifiButton);
        wifiButton.addActionListener(e -> showWifiScanDialog());
        buttonPanel.add(wifiButton);

        refreshButton = new JButton("🔄 Refresh");
        UITheme.styleNeutralButton(refreshButton);
        refreshButton.addActionListener(e -> loadDevices());
        buttonPanel.add(refreshButton);

        topPanel.add(buttonPanel, BorderLayout.EAST);
        return topPanel;
    }

    private JPanel createTablePanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        UITheme.styleCard(centerPanel);

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableModel.setColumnIdentifiers(new String[]{
            "ID", "Device Name", "IP Address", "MAC Address", "Type", "Location", "Status"
        });

        deviceTable = new JTable(tableModel);
        UITheme.styleTable(deviceTable);

        deviceTable.getColumnModel().getColumn(COL_ID).setPreferredWidth(40);
        deviceTable.getColumnModel().getColumn(COL_NAME).setPreferredWidth(140);
        deviceTable.getColumnModel().getColumn(COL_IP).setPreferredWidth(110);
        deviceTable.getColumnModel().getColumn(COL_MAC).setPreferredWidth(130);
        deviceTable.getColumnModel().getColumn(COL_TYPE).setPreferredWidth(110);
        deviceTable.getColumnModel().getColumn(COL_LOCATION).setPreferredWidth(120);
        deviceTable.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(100);

        deviceTable.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(deviceTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        return centerPanel;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setOpaque(false);

        statusLabel = new JLabel("Total registered devices: 0");
        statusLabel.setFont(UITheme.FONT_BODY_BOLD);
        statusLabel.setForeground(UITheme.TEXT_MUTED);
        bottomPanel.add(statusLabel);

        return bottomPanel;
    }

    private void loadDevices() {
        try {
            tableModel.setRowCount(0);
            List<Device> devices = deviceDAO.getAllDevices();

            for (Device device : devices) {
                tableModel.addRow(new Object[]{
                    device.getDeviceId(),
                    device.getDeviceName(),
                    device.getIpAddress(),
                    device.getMacAddress() != null ? device.getMacAddress() : "-",
                    device.getDeviceType(),
                    device.getLocation() != null ? device.getLocation() : "-",
                    device.getStatus()
                });
            }

            statusLabel.setText("Total registered devices: " + devices.size());
        } catch (Exception e) {
            showError("Error loading devices: " + e.getMessage());
        }
    }

    private void showAddDeviceDialog() {
        DeviceDialog dialog = new DeviceDialog((Frame) SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            Device newDevice = dialog.getDevice();
            newDevice.setAddedBy(currentUser.getUserId());
            if (deviceDAO.addDevice(newDevice)) {
                loadDevices();
                com.networkmonitor.service.DeviceCache.getInstance().invalidate();
                JOptionPane.showMessageDialog(this, "Device added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                showError("Failed to save device to database");
            }
        }
    }

    private void showEditDeviceDialog() {
        int selectedRow = deviceTable.getSelectedRow();
        if (selectedRow == -1) {
            showWarning("Please select a device from the table to edit.");
            return;
        }

        int deviceId = (Integer) tableModel.getValueAt(selectedRow, COL_ID);
        Device device = deviceDAO.getDeviceById(deviceId);

        if (device == null) {
            showError("Could not retrieve device details.");
            return;
        }

        DeviceDialog dialog = new DeviceDialog((Frame) SwingUtilities.getWindowAncestor(this), device);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            Device updatedDevice = dialog.getDevice();
            if (deviceDAO.updateDevice(updatedDevice)) {
                loadDevices();
                com.networkmonitor.service.DeviceCache.getInstance().invalidate();
                JOptionPane.showMessageDialog(this, "Device updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                showError("Failed to update device");
            }
        }
    }

    private void showDeleteConfirmation() {
        int selectedRow = deviceTable.getSelectedRow();
        if (selectedRow == -1) {
            showWarning("Please select a device to delete.");
            return;
        }

        int deviceId = (Integer) tableModel.getValueAt(selectedRow, COL_ID);
        String deviceName = (String) tableModel.getValueAt(selectedRow, COL_NAME);

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete device '" + deviceName + "'?\nThis will remove associated metrics.",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (deviceDAO.deleteDevice(deviceId)) {
                loadDevices();
                com.networkmonitor.service.DeviceCache.getInstance().invalidate();
                JOptionPane.showMessageDialog(this, "Device deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                showError("Failed to delete device.");
            }
        }
    }

    private void showWifiScanDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Available WiFi Networks", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel title = new JLabel("📡 Scanning nearby networks...");
        title.setFont(UITheme.FONT_HEADER);
        headerPanel.add(title);
        dialog.add(headerPanel, BorderLayout.NORTH);

        DefaultTableModel wifiModel = new DefaultTableModel(new String[]{"SSID", "BSSID", "Signal", "Channel", "Security", "Connected"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable wifiTable = new JTable(wifiModel);
        UITheme.styleTable(wifiTable);
        JScrollPane scrollPane = new JScrollPane(wifiTable);
        scrollPane.getViewport().setBackground(Color.WHITE);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(Color.WHITE);
        JButton closeBtn = new JButton("Close");
        UITheme.styleNeutralButton(closeBtn);
        closeBtn.addActionListener(e -> dialog.dispose());
        
        JButton refreshBtn = new JButton("Scan Again");
        UITheme.stylePrimaryButton(refreshBtn);
        
        btnPanel.add(refreshBtn);
        btnPanel.add(closeBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        // Perform scan on background thread
        Runnable scanTask = () -> {
            refreshBtn.setEnabled(false);
            title.setText("📡 Scanning nearby networks... (This may take a few seconds)");
            wifiModel.setRowCount(0);
            
            new SwingWorker<List<com.networkmonitor.util.NetworkAdapter.AvailableNetwork>, Void>() {
                @Override
                protected List<com.networkmonitor.util.NetworkAdapter.AvailableNetwork> doInBackground() {
                    return com.networkmonitor.util.NetworkAdapter.scanAvailableNetworks();
                }

                @Override
                protected void done() {
                    try {
                        List<com.networkmonitor.util.NetworkAdapter.AvailableNetwork> networks = get();
                        for (var net : networks) {
                            wifiModel.addRow(new Object[]{
                                net.ssid,
                                net.bssid,
                                net.signalStrength + "%",
                                net.channel,
                                net.securityType,
                                net.isConnected ? "🟢 YES" : "⚪ NO"
                            });
                        }
                        title.setText("📡 Found " + networks.size() + " networks");
                    } catch (Exception ex) {
                        title.setText("❌ Error scanning networks");
                        JOptionPane.showMessageDialog(dialog, "Failed to scan networks: " + ex.getMessage());
                    }
                    refreshBtn.setEnabled(true);
                }
            }.execute();
        };

        refreshBtn.addActionListener(e -> scanTask.run());
        scanTask.run(); // initial scan

        dialog.setVisible(true);
    }

    private JPanel createFieldRow(String labelText, JComponent comp) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UITheme.FONT_BODY_BOLD);
        lbl.setForeground(UITheme.TEXT_PRIMARY);

        p.add(lbl);
        p.add(Box.createVerticalStrut(3));
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        p.add(comp);
        p.add(Box.createVerticalStrut(8));
        return p;
    }

    private boolean isOperatorOrAdmin() {
        String role = currentUser.getRole();
        return "ADMIN".equals(role) || "OPERATOR".equals(role);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * High contrast cell renderer for Status Column
     */
    private static class StatusCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        StatusCellRenderer() {
            setHorizontalAlignment(CENTER);
            setFont(UITheme.FONT_BODY_BOLD);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = (String) value;

            if ("ONLINE".equals(status)) {
                if (!isSelected) setForeground(UITheme.SUCCESS_GREEN);
                setText("🟢 ONLINE");
            } else if ("OFFLINE".equals(status)) {
                if (!isSelected) setForeground(UITheme.DANGER_RED);
                setText("🔴 OFFLINE");
            } else {
                if (!isSelected) setForeground(UITheme.WARNING_ORANGE);
                setText("🟡 WARNING");
            }

            return this;
        }
    }
}
