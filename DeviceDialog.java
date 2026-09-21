package com.networkmonitor.ui;

import com.networkmonitor.dao.DeviceDAO;
import com.networkmonitor.model.Device;
import com.networkmonitor.util.NetworkAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;

/**
 * DeviceDialog - Add/Edit Device Dialog
 *
 * Modal dialog for adding new devices or editing existing devices.
 * Includes form validation and interface selection.
 */
public class DeviceDialog extends JDialog {

    private Device device;
    private boolean confirmed = false;

    // Form fields
    private JTextField nameField;
    private JTextField ipField;
    private JComboBox<String> typeCombo;
    private JComboBox<String> interfaceCombo;
    private JTextField locationField;
    private JTextArea descriptionArea;

    // Buttons
    private JButton saveButton;
    private JButton cancelButton;

    /**
     * Constructor for adding new device
     */
    public DeviceDialog(Frame parent) {
        this(parent, null);
    }

    /**
     * Constructor for editing existing device
     */
    public DeviceDialog(Frame parent, Device device) {
        super(parent, device == null ? "Add New Device" : "Edit Device", true);
        this.device = device;

        initializeUI();
        populateInterfaceCombo();

        if (device != null) {
            populateFields();
        }

        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));

        // Main form panel
        JPanel formPanel = createFormPanel();
        add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);

        setSize(500, 550);
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        // Device Name
        addFormRow(panel, gbc, row++, "Device Name *",
            nameField = new JTextField(20),
            "A friendly name to identify this device");

        // IP Address
        addFormRow(panel, gbc, row++, "IP Address *",
            ipField = new JTextField(20),
            "IPv4 address of the device (e.g., 192.168.1.1)");

        // Device Type
        String[] types = {"", "Router", "Switch", "Server", "PC", "Mobile", "Firewall", "Access Point", "Other"};
        typeCombo = new JComboBox<>(types);
        addFormRow(panel, gbc, row++, "Device Type *",
            typeCombo,
            "Select the type of network device");

        // Network Interface
        interfaceCombo = new JComboBox<>();
        interfaceCombo.addItem("Auto-detect");
        addFormRow(panel, gbc, row++, "Network Interface",
            interfaceCombo,
            "Map to a specific network interface (optional)");

        // Location
        addFormRow(panel, gbc, row++, "Location",
            locationField = new JTextField(20),
            "Physical location of the device (optional)");

        // Description
        descriptionArea = new JTextArea(4, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        addFormRow(panel, gbc, row++, "Description",
            descScroll,
            "Additional notes about this device (optional)");

        return panel;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row,
                           String labelText, Component field, String helpText) {
        // Label
        gbc.gridx = 0;
        gbc.gridy = row * 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(new Color(15, 23, 42)); // Dark navy - always readable
        panel.add(label, gbc);

        // Field
        gbc.gridx = 0;
        gbc.gridy = row * 2 + 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        panel.add(field, gbc);

        // Help text
        if (helpText != null) {
            gbc.gridy = row * 2 + 1;
            JLabel help = new JLabel("<html><small>" + helpText + "</small></html>");
            help.setForeground(new Color(71, 85, 105)); // Darker muted text
            panel.add(help, gbc);
        }
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        saveButton = new JButton(device == null ? "💾 Save Device" : "💾 Update Device");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveButton.setBackground(new Color(37, 99, 235));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.addActionListener(e -> saveDevice());

        panel.add(cancelButton);
        panel.add(saveButton);

        return panel;
    }

    private void populateInterfaceCombo() {
        try {
            var stats = NetworkAdapter.getAllNetworkStats();
            for (var iface : stats.interfaces) {
                String entry = String.format("%s (%s) - %s",
                    iface.displayName, iface.name, iface.ipAddress);
                interfaceCombo.addItem(entry);
            }
        } catch (Exception e) {
            System.err.println("Error loading interfaces: " + e.getMessage());
        }
    }

    private void populateFields() {
        nameField.setText(device.getDeviceName());
        ipField.setText(device.getIpAddress());

        // Set device type
        String type = device.getDeviceType();
        if (type != null) {
            type = type.substring(0, 1).toUpperCase() + type.substring(1);
            typeCombo.setSelectedItem(type);
        }

        // Set interface
        if (device.getNetworkInterface() != null) {
            for (int i = 0; i < interfaceCombo.getItemCount(); i++) {
                String item = interfaceCombo.getItemAt(i);
                if (item.contains(device.getNetworkInterface())) {
                    interfaceCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        if (device.getLocation() != null) {
            locationField.setText(device.getLocation());
        }

        if (device.getDescription() != null) {
            descriptionArea.setText(device.getDescription());
        }
    }

    private void saveDevice() {
        // Validate required fields
        if (nameField.getText().trim().isEmpty()) {
            showError("Please enter a device name");
            nameField.requestFocus();
            return;
        }

        if (ipField.getText().trim().isEmpty()) {
            showError("Please enter an IP address");
            ipField.requestFocus();
            return;
        }

        // Validate IP format
        if (!isValidIP(ipField.getText().trim())) {
            showError("Please enter a valid IPv4 address (e.g., 192.168.1.1)");
            ipField.requestFocus();
            return;
        }

        if (typeCombo.getSelectedIndex() == 0) {
            showError("Please select a device type");
            typeCombo.requestFocus();
            return;
        }

        // Create or update device
        if (device == null) {
            device = new Device();
        }

        device.setDeviceName(nameField.getText().trim());
        device.setIpAddress(ipField.getText().trim());
        device.setDeviceType(typeCombo.getSelectedItem().toString().toLowerCase());

        // Set interface (extract interface name from combo item)
        String selectedInterface = interfaceCombo.getSelectedItem().toString();
        if (!selectedInterface.equals("Auto-detect") && selectedInterface.contains("(")) {
            String ifaceName = selectedInterface.substring(
                selectedInterface.indexOf("(") + 1,
                selectedInterface.indexOf(")")
            );
            device.setNetworkInterface(ifaceName);
        }

        device.setLocation(locationField.getText().trim());
        device.setDescription(descriptionArea.getText().trim());
        device.setStatus("ONLINE");
        device.setLastSeen(new java.sql.Timestamp(System.currentTimeMillis()));

        confirmed = true;
        dispose();
    }

    private boolean isValidIP(String ip) {
        String ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(ipPattern);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
            message,
            "Validation Error",
            JOptionPane.ERROR_MESSAGE);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Device getDevice() {
        return device;
    }
}
