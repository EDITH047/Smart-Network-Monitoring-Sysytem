package com.networkmonitor.ui;

import com.networkmonitor.dao.DeviceDAO;
import com.networkmonitor.model.Device;
import com.networkmonitor.model.User;
import com.networkmonitor.util.ValidationUtil;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

/**
 * DevicePanel - Device management UI with full CRUD operations
 * Add, edit, delete network devices with validation and real-time updates
 */
public class DevicePanel extends JPanel implements ThemeManager.ThemeListener {

    private User currentUser;
    private DeviceDAO deviceDAO;
    private JTable deviceTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private JLabel statusLabel;
    
    private JPanel topPanel;
    private JPanel centerPanel;
    private JPanel bottomPanel;
    private JPanel buttonPanel;
    private JLabel titleLabel;
    private JScrollPane scrollPane;

    // Table column indices
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
        ThemeManager.addThemeListener(this);
        applyTheme();
    }
    
    @Override
    public void onThemeChanged() {
        applyTheme();
    }
    
    private void applyTheme() {
        setBackground(ThemeManager.getBackgroundColor());
        topPanel.setBackground(ThemeManager.getBackgroundColor());
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());
        centerPanel.setBackground(ThemeManager.getCardColor());
        bottomPanel.setBackground(ThemeManager.getBorderColor());
        
        titleLabel.setForeground(ThemeManager.getTextColor());
        
        addButton.setBackground(ThemeManager.getSuccessColor());
        editButton.setBackground(ThemeManager.getPrimaryColor());
        deleteButton.setBackground(ThemeManager.getErrorColor());
        refreshButton.setBackground(ThemeManager.getTextMutedColor());
        
        deviceTable.setBackground(ThemeManager.getCardColor());
        deviceTable.setForeground(ThemeManager.getTextColor());
        deviceTable.getTableHeader().setBackground(ThemeManager.getPrimaryColor());
        deviceTable.getTableHeader().setForeground(Color.WHITE);
        deviceTable.setGridColor(ThemeManager.getBorderColor());
        
        scrollPane.setBackground(ThemeManager.getCardColor());
        scrollPane.getViewport().setBackground(ThemeManager.getCardColor());
        
        statusLabel.setForeground(ThemeManager.getTextMutedColor());
        
        repaint();
    }

    /**
     * Initialize UI components
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top panel - Title and buttons
        topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // Center panel - Table
        centerPanel = createTablePanel();
        add(centerPanel, BorderLayout.CENTER);

        // Bottom panel - Status
        bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Create top panel with title and action buttons
     */
    private JPanel createTopPanel() {
        JPanel pnl = new JPanel(new BorderLayout(10, 0));

        // Title
        titleLabel = new JLabel("📱 Network Device Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        pnl.add(titleLabel, BorderLayout.WEST);

        // Button panel
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        // Add button
        addButton = new JButton("➕ Add Device");
        addButton.setFont(new Font("Arial", Font.BOLD, 11));
        addButton.setForeground(Color.WHITE);
        addButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        addButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addButton.setEnabled(isOperatorOrAdmin());
        addButton.addActionListener(e -> showAddDeviceDialog());
        buttonPanel.add(addButton);

        // Edit button
        editButton = new JButton("✏️ Edit");
        editButton.setFont(new Font("Arial", Font.BOLD, 11));
        editButton.setForeground(Color.WHITE);
        editButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        editButton.setEnabled(isOperatorOrAdmin());
        editButton.addActionListener(e -> showEditDeviceDialog());
        buttonPanel.add(editButton);

        // Delete button
        deleteButton = new JButton("🗑️ Delete");
        deleteButton.setFont(new Font("Arial", Font.BOLD, 11));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteButton.setEnabled(isOperatorOrAdmin());
        deleteButton.addActionListener(e -> showDeleteConfirmation());
        buttonPanel.add(deleteButton);

        // Refresh button
        refreshButton = new JButton("🔄 Refresh");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 11));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> loadDevices());
        buttonPanel.add(refreshButton);

        pnl.add(buttonPanel, BorderLayout.EAST);

        return pnl;
    }

    /**
     * Create center panel with JTable
     */
    private JPanel createTablePanel() {
        JPanel pnl = new JPanel(new BorderLayout());

        // Create table model
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Table is read-only, editing done via dialog
            }
        };

        tableModel.setColumnIdentifiers(new String[]{
                "ID", "Device Name", "IP Address", "MAC Address", "Type", "Location", "Status"
        });

        // Create table
        deviceTable = new JTable(tableModel);
        deviceTable.setFont(new Font("Arial", Font.PLAIN, 11));
        deviceTable.setRowHeight(25);
        deviceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deviceTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 11));

        // Set column widths
        deviceTable.getColumnModel().getColumn(COL_ID).setPreferredWidth(40);
        deviceTable.getColumnModel().getColumn(COL_NAME).setPreferredWidth(120);
        deviceTable.getColumnModel().getColumn(COL_IP).setPreferredWidth(100);
        deviceTable.getColumnModel().getColumn(COL_MAC).setPreferredWidth(110);
        deviceTable.getColumnModel().getColumn(COL_TYPE).setPreferredWidth(100);
        deviceTable.getColumnModel().getColumn(COL_LOCATION).setPreferredWidth(100);
        deviceTable.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(80);

        // Color-coded status renderer
        deviceTable.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new StatusCellRenderer());

        // Scroll pane
        scrollPane = new JScrollPane(deviceTable);
        scrollPane.setBorder(null);
        pnl.add(scrollPane, BorderLayout.CENTER);

        return pnl;
    }

    /**
     * Create bottom panel with status label
     */
    private JPanel createBottomPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnl.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        statusLabel = new JLabel("Total devices: 0");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        pnl.add(statusLabel);

        return pnl;
    }

    /**
     * Load all devices from database
     */
    private void loadDevices() {
        try {
            tableModel.setRowCount(0); // Clear table

            List<Device> devices = deviceDAO.getAllDevices();

            for (Device device : devices) {
                tableModel.addRow(new Object[]{
                        device.getDeviceId(),
                        device.getDeviceName(),
                        device.getIpAddress(),
                        device.getMacAddress(),
                        device.getDeviceType(),
                        device.getLocation(),
                        device.getStatus()
                });
            }

            statusLabel.setText("Total devices: " + devices.size());
            System.out.println("[DevicePanel] Loaded " + devices.size() + " devices");

        } catch (Exception e) {
            showError("Error loading devices: " + e.getMessage());
            System.err.println("[DevicePanel] Error loading devices: " + e.getMessage());
        }
    }

    /**
     * Show add device dialog
     */
    private void showAddDeviceDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add New Device", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(ThemeManager.getBackgroundColor());

        // Device Name
        mainPanel.add(createLabel("Device Name:"));
        JTextField nameField = new JTextField();
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        mainPanel.add(nameField);
        mainPanel.add(Box.createVerticalStrut(10));

        // IP Address
        mainPanel.add(createLabel("IP Address:"));
        JTextField ipField = new JTextField();
        ipField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        mainPanel.add(ipField);
        mainPanel.add(Box.createVerticalStrut(10));

        // MAC Address
        mainPanel.add(createLabel("MAC Address:"));
        JTextField macField = new JTextField();
        macField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        mainPanel.add(macField);
        mainPanel.add(Box.createVerticalStrut(10));

        // Device Type
        mainPanel.add(createLabel("Device Type:"));
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"ROUTER", "SWITCH", "SERVER", "ACCESS_POINT", "FIREWALL"});
        typeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        mainPanel.add(typeCombo);
        mainPanel.add(Box.createVerticalStrut(10));

        // Location
        mainPanel.add(createLabel("Location:"));
        JTextField locationField = new JTextField();
        locationField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        mainPanel.add(locationField);
        mainPanel.add(Box.createVerticalStrut(20));

        // Error label
        JLabel errorLabel = new JLabel();
        errorLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        errorLabel.setForeground(ThemeManager.getErrorColor());
        mainPanel.add(errorLabel);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());

        JButton saveButton = new JButton("Save");
        saveButton.setBackground(ThemeManager.getSuccessColor());
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String ip = ipField.getText().trim();
            String mac = macField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();
            String location = locationField.getText().trim();

            // Validate inputs
            if (name.isEmpty() || ip.isEmpty()) {
                errorLabel.setText("Device name and IP address are required");
                return;
            }

            if (!ValidationUtil.isValidIPv4(ip)) {
                errorLabel.setText("Invalid IP address format");
                return;
            }

            if (!mac.isEmpty() && !ValidationUtil.isValidMac(mac)) {
                errorLabel.setText("Invalid MAC address format");
                return;
            }

            // Create device
            Device device = new Device(name, ip, mac, type, location, currentUser.getUserId());
            if (deviceDAO.addDevice(device)) {
                loadDevices();
                dialog.dispose();
                JOptionPane.showMessageDialog(DevicePanel.this, "Device added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                errorLabel.setText("Failed to add device");
            }
        });
        buttonPanel.add(saveButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(ThemeManager.getTextMutedColor());
        cancelButton.setForeground(Color.WHITE);
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel);
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    /**
     * Show edit device dialog
     */
    private void showEditDeviceDialog() {
        int selectedRow = deviceTable.getSelectedRow();
        if (selectedRow == -1) {
            showWarning("Please select a device to edit");
            return;
        }

        int deviceId = (Integer) tableModel.getValueAt(selectedRow, COL_ID);
        Device device = deviceDAO.getDeviceById(deviceId);

        if (device == null) {
            showError("Could not load device");
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Device", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(ThemeManager.getBackgroundColor());

        // Device Name
        mainPanel.add(createLabel("Device Name:"));
        JTextField nameField = new JTextField(device.getDeviceName());
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        mainPanel.add(nameField);
        mainPanel.add(Box.createVerticalStrut(10));

        // MAC Address
        mainPanel.add(createLabel("MAC Address:"));
        JTextField macField = new JTextField(device.getMacAddress() != null ? device.getMacAddress() : "");
        macField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        mainPanel.add(macField);
        mainPanel.add(Box.createVerticalStrut(10));

        // Device Type
        mainPanel.add(createLabel("Device Type:"));
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"ROUTER", "SWITCH", "SERVER", "ACCESS_POINT", "FIREWALL"});
        typeCombo.setSelectedItem(device.getDeviceType());
        typeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        mainPanel.add(typeCombo);
        mainPanel.add(Box.createVerticalStrut(10));

        // Location
        mainPanel.add(createLabel("Location:"));
        JTextField locationField = new JTextField(device.getLocation() != null ? device.getLocation() : "");
        locationField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        mainPanel.add(locationField);
        mainPanel.add(Box.createVerticalStrut(10));

        // Status
        mainPanel.add(createLabel("Status:"));
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ONLINE", "OFFLINE", "WARNING"});
        statusCombo.setSelectedItem(device.getStatus());
        statusCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        mainPanel.add(statusCombo);
        mainPanel.add(Box.createVerticalStrut(20));

        // Error label
        JLabel errorLabel = new JLabel();
        errorLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        errorLabel.setForeground(ThemeManager.getErrorColor());
        mainPanel.add(errorLabel);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());

        JButton saveButton = new JButton("Save");
        saveButton.setBackground(ThemeManager.getSuccessColor());
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String mac = macField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();
            String location = locationField.getText().trim();
            String status = (String) statusCombo.getSelectedItem();

            if (name.isEmpty()) {
                errorLabel.setText("Device name is required");
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
        buttonPanel.add(saveButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(ThemeManager.getTextMutedColor());
        cancelButton.setForeground(Color.WHITE);
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel);
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    /**
     * Show delete confirmation
     */
    private void showDeleteConfirmation() {
        int selectedRow = deviceTable.getSelectedRow();
        if (selectedRow == -1) {
            showWarning("Please select a device to delete");
            return;
        }

        int deviceId = (Integer) tableModel.getValueAt(selectedRow, COL_ID);
        String deviceName = (String) tableModel.getValueAt(selectedRow, COL_NAME);

        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete device '" + deviceName + "'?\nThis action cannot be undone.",
                "Delete Device",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            if (deviceDAO.deleteDevice(deviceId)) {
                loadDevices();
                JOptionPane.showMessageDialog(this, "Device deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                showError("Failed to delete device");
            }
        }
    }

    /**
     * Check if user is operator or admin
     */
    private boolean isOperatorOrAdmin() {
        String role = currentUser.getRole();
        return "ADMIN".equals(role) || "OPERATOR".equals(role);
    }

    /**
     * Create label
     */
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 11));
        label.setForeground(ThemeManager.getTextColor());
        return label;
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Show warning message
     */
    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Custom renderer for status column with color coding
     */
    private static class StatusCellRenderer extends JLabel implements TableCellRenderer {
        StatusCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setFont(new Font("Arial", Font.BOLD, 11));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            String status = (String) value;

            if ("ONLINE".equals(status)) {
                setBackground(ThemeManager.getSuccessBgColor());
                setForeground(ThemeManager.getSuccessColor());
                setText("🟢 " + status);
            } else if ("OFFLINE".equals(status)) {
                setBackground(ThemeManager.getErrorBgColor());
                setForeground(ThemeManager.getErrorColor());
                setText("🔴 " + status);
            } else if ("WARNING".equals(status)) {
                setBackground(ThemeManager.getWarningBgColor());
                setForeground(ThemeManager.getWarningColor());
                setText("🟡 " + status);
            }

            if (isSelected) {
                setBackground(ThemeManager.getPrimaryColor());
                setForeground(Color.WHITE);
                setText(status);
            }

            return this;
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        ThemeManager.removeThemeListener(this);
    }
}
