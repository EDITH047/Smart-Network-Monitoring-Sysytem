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
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add New Device", true);
        dialog.setSize(440, 420);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        mainPanel.setBackground(UITheme.CARD_BG);

        JTextField nameField = new JTextField();
        JTextField ipField = new JTextField();
        JTextField macField = new JTextField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"ROUTER", "SWITCH", "SERVER", "ACCESS_POINT", "FIREWALL"});
        JTextField locationField = new JTextField();

        UITheme.styleTextField(nameField);
        UITheme.styleTextField(ipField);
        UITheme.styleTextField(macField);
        UITheme.styleTextField(locationField);

        mainPanel.add(createFieldRow("Device Name *", nameField));
        mainPanel.add(createFieldRow("IP Address *", ipField));
        mainPanel.add(createFieldRow("MAC Address", macField));
        mainPanel.add(createFieldRow("Device Type", typeCombo));
        mainPanel.add(createFieldRow("Location", locationField));

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.DANGER_RED);
        mainPanel.add(errorLabel);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        JButton saveBtn = new JButton("Save Device");
        UITheme.styleSuccessButton(saveBtn);

        JButton cancelBtn = new JButton("Cancel");
        UITheme.styleNeutralButton(cancelBtn);
        cancelBtn.addActionListener(e -> dialog.dispose());

        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String ip = ipField.getText().trim();
            String mac = macField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();
            String location = locationField.getText().trim();

            if (name.isEmpty() || ip.isEmpty()) {
                errorLabel.setText("Device Name and IP Address are required");
                return;
            }

            if (!ValidationUtil.isValidIPv4(ip)) {
                errorLabel.setText("Invalid IPv4 address format (e.g. 192.168.1.1)");
                return;
            }

            if (!mac.isEmpty() && !ValidationUtil.isValidMac(mac)) {
                errorLabel.setText("Invalid MAC address format (e.g. AA:BB:CC:DD:EE:FF)");
                return;
            }

            Device device = new Device(name, ip, mac, type, location, currentUser.getUserId());
            if (deviceDAO.addDevice(device)) {
                loadDevices();
                dialog.dispose();
                JOptionPane.showMessageDialog(DevicePanel.this, "Device added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                errorLabel.setText("Failed to save device to database");
            }
        });

        btnRow.add(saveBtn);
        btnRow.add(cancelBtn);
        mainPanel.add(btnRow);

        dialog.add(mainPanel);
        dialog.setVisible(true);
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

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Device — " + device.getDeviceName(), true);
        dialog.setSize(440, 440);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        mainPanel.setBackground(UITheme.CARD_BG);

        JTextField nameField = new JTextField(device.getDeviceName());
        JTextField macField = new JTextField(device.getMacAddress() != null ? device.getMacAddress() : "");
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"ROUTER", "SWITCH", "SERVER", "ACCESS_POINT", "FIREWALL"});
        typeCombo.setSelectedItem(device.getDeviceType());
        JTextField locationField = new JTextField(device.getLocation() != null ? device.getLocation() : "");
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ONLINE", "OFFLINE", "WARNING"});
        statusCombo.setSelectedItem(device.getStatus());

        UITheme.styleTextField(nameField);
        UITheme.styleTextField(macField);
        UITheme.styleTextField(locationField);

        mainPanel.add(createFieldRow("Device Name *", nameField));
        mainPanel.add(createFieldRow("MAC Address", macField));
        mainPanel.add(createFieldRow("Device Type", typeCombo));
        mainPanel.add(createFieldRow("Location", locationField));
        mainPanel.add(createFieldRow("Status", statusCombo));

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.DANGER_RED);
        mainPanel.add(errorLabel);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        JButton saveBtn = new JButton("Update Device");
        UITheme.stylePrimaryButton(saveBtn);

        JButton cancelBtn = new JButton("Cancel");
        UITheme.styleNeutralButton(cancelBtn);
        cancelBtn.addActionListener(e -> dialog.dispose());

        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String mac = macField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();
            String location = locationField.getText().trim();
            String status = (String) statusCombo.getSelectedItem();

            if (name.isEmpty()) {
                errorLabel.setText("Device Name cannot be empty");
                return;
            }

            if (!mac.isEmpty() && !ValidationUtil.isValidMac(mac)) {
                errorLabel.setText("Invalid MAC address format");
                return;
            }

            device.setDeviceName(name);
            device.setMacAddress(mac);
            device.setDeviceType(type);
            device.setLocation(location);
            device.setStatus(status);

            if (deviceDAO.updateDevice(device)) {
                loadDevices();
                dialog.dispose();
                JOptionPane.showMessageDialog(DevicePanel.this, "Device updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                errorLabel.setText("Failed to update device");
            }
        });

        btnRow.add(saveBtn);
        btnRow.add(cancelBtn);
        mainPanel.add(btnRow);

        dialog.add(mainPanel);
        dialog.setVisible(true);
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
                JOptionPane.showMessageDialog(this, "Device deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                showError("Failed to delete device.");
            }
        }
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
    private static class StatusCellRenderer extends JLabel implements TableCellRenderer {
        StatusCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setFont(UITheme.FONT_BODY_BOLD);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            String status = (String) value;

            if ("ONLINE".equals(status)) {
                setBackground(new Color(220, 252, 231));
                setForeground(UITheme.SUCCESS_GREEN);
                setText("🟢 ONLINE");
            } else if ("OFFLINE".equals(status)) {
                setBackground(new Color(254, 226, 226));
                setForeground(UITheme.DANGER_RED);
                setText("🔴 OFFLINE");
            } else {
                setBackground(new Color(254, 243, 199));
                setForeground(UITheme.WARNING_ORANGE);
                setText("🟡 WARNING");
            }

            if (isSelected) {
                setBackground(UITheme.PRIMARY_BLUE);
                setForeground(UITheme.TEXT_LIGHT);
            }

            return this;
        }
    }
}
