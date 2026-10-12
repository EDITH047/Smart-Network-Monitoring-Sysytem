package com.networkmonitor.ui;

import com.networkmonitor.dao.AuditLogDAO;
import com.networkmonitor.dao.DeviceDAO;
import com.networkmonitor.model.AuditLog;
import com.networkmonitor.model.Device;
import com.networkmonitor.model.User;
import com.networkmonitor.service.DeviceCache;
import com.networkmonitor.service.SubnetScanner;
import com.networkmonitor.service.SubnetScanner.DiscoveredHost;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * SubnetScanDialog — Interactive auto-discovery wizard allowing administrators
 * to sweep local IP subnets, view responding nodes, and import them with 1 click.
 */
public class SubnetScanDialog extends JDialog {

    private final User currentUser;
    private final DeviceDAO deviceDAO = new DeviceDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private JTextField prefixField;
    private JSpinner startSpinner;
    private JSpinner endSpinner;
    private JButton startBtn;
    private JButton stopBtn;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton importBtn;

    private ExecutorService scanExecutor;
    private Future<?> activeScanFuture;
    private final List<DiscoveredHost> currentDiscovered = new ArrayList<>();
    private boolean onImportSuccessCallbackTriggered = false;

    public SubnetScanDialog(Window parent, User currentUser) {
        super(parent, "🔍 Network Subnet IP Sweep & Auto-Discovery", ModalityType.APPLICATION_MODAL);
        this.currentUser = currentUser;
        this.scanExecutor = Executors.newFixedThreadPool(32);

        initializeUI();
        setSize(880, 560);
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        JPanel contentPane = new JPanel(new BorderLayout(12, 12));
        contentPane.setBackground(UITheme.BG_CANVAS);
        contentPane.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Top Control Panel
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        controls.setOpaque(false);

        controls.add(new JLabel("Subnet Prefix:"));
        prefixField = new JTextField(SubnetScanner.detectLocalSubnetPrefix(), 11);
        prefixField.setFont(UITheme.FONT_BODY);
        controls.add(prefixField);

        controls.add(new JLabel("Host Range:"));
        startSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 254, 1));
        endSpinner = new JSpinner(new SpinnerNumberModel(254, 1, 254, 1));
        controls.add(startSpinner);
        controls.add(new JLabel("to"));
        controls.add(endSpinner);

        startBtn = new JButton("🚀 Start Sweep");
        UITheme.stylePrimaryButton(startBtn);
        startBtn.addActionListener(e -> startScan());
        controls.add(startBtn);

        stopBtn = new JButton("🛑 Stop");
        UITheme.styleDangerButton(stopBtn);
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> stopScan());
        controls.add(stopBtn);

        topPanel.add(controls, BorderLayout.NORTH);

        // Progress panel
        JPanel progressPanel = new JPanel(new BorderLayout(8, 4));
        progressPanel.setOpaque(false);
        progressBar = new JProgressBar(0, 254);
        progressBar.setStringPainted(true);
        progressBar.setForeground(UITheme.PRIMARY_BLUE);
        statusLabel = new JLabel("Ready to sweep. Click 'Start Sweep' to scan subnet.");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);
        progressPanel.add(progressBar, BorderLayout.NORTH);
        progressPanel.add(statusLabel, BorderLayout.SOUTH);

        topPanel.add(progressPanel, BorderLayout.SOUTH);
        contentPane.add(topPanel, BorderLayout.NORTH);

        // Center Table
        tableModel = new DefaultTableModel(new String[]{
            "Select", "IP Address", "Host Name", "MAC Address", "Type", "Latency"
        }, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };

        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(140);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(90);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new BorderLayout(10, 0));
        bottomBar.setOpaque(false);

        JPanel selectBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        selectBtns.setOpaque(false);
        JButton selectAllBtn = new JButton("Select All");
        UITheme.styleNeutralButton(selectAllBtn);
        selectAllBtn.addActionListener(e -> setAllSelected(true));
        JButton deselectAllBtn = new JButton("Deselect All");
        UITheme.styleNeutralButton(deselectAllBtn);
        deselectAllBtn.addActionListener(e -> setAllSelected(false));
        selectBtns.add(selectAllBtn);
        selectBtns.add(deselectAllBtn);
        bottomBar.add(selectBtns, BorderLayout.WEST);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBtns.setOpaque(false);

        importBtn = new JButton("📥 Import Selected Devices (0)");
        UITheme.styleSuccessButton(importBtn);
        importBtn.setEnabled(false);
        importBtn.addActionListener(e -> importSelectedDevices());
        actionBtns.add(importBtn);

        JButton closeBtn = new JButton("Close");
        UITheme.styleNeutralButton(closeBtn);
        closeBtn.addActionListener(e -> {
            stopScan();
            dispose();
        });
        actionBtns.add(closeBtn);

        bottomBar.add(actionBtns, BorderLayout.EAST);
        contentPane.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(contentPane);
    }

    private void startScan() {
        String prefix = prefixField.getText().trim();
        if (!prefix.endsWith(".")) prefix += ".";
        int start = (Integer) startSpinner.getValue();
        int end = (Integer) endSpinner.getValue();
        if (start > end) {
            JOptionPane.showMessageDialog(this, "Start host cannot exceed End host.", "Invalid Range", JOptionPane.WARNING_MESSAGE);
            return;
        }

        currentDiscovered.clear();
        tableModel.setRowCount(0);
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        prefixField.setEnabled(false);
        importBtn.setEnabled(false);
        importBtn.setText("📥 Import Selected Devices (0)");

        int total = end - start + 1;
        progressBar.setMaximum(total);
        progressBar.setValue(0);
        statusLabel.setText(String.format("Sweeping %s%d to %d... (0/%d)", prefix, start, end, total));

        activeScanFuture = SubnetScanner.scanSubnetAsync(prefix, start, end, new SubnetScanner.ScanProgressListener() {
            @Override
            public void onHostDiscovered(DiscoveredHost host) {
                SwingUtilities.invokeLater(() -> {
                    currentDiscovered.add(host);
                    boolean alreadyExists = deviceDAO.deviceExistsByIp(host.getIpAddress());
                    // Check by default if not already in database
                    boolean selectDefault = !alreadyExists;
                    tableModel.addRow(new Object[]{
                        selectDefault,
                        host.getIpAddress() + (alreadyExists ? " (Exists)" : ""),
                        host.getHostName(),
                        host.getMacAddress(),
                        host.getDeviceType().toUpperCase(),
                        host.getLatencyMs() + " ms"
                    });
                    updateImportButtonCount();
                });
            }

            @Override
            public void onProgress(int current, int totalCount) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(current);
                    statusLabel.setText(String.format("Sweeping subnet... %d/%d hosts scanned | %d alive nodes found",
                            current, totalCount, currentDiscovered.size()));
                });
            }

            @Override
            public void onScanCompleted(List<DiscoveredHost> results) {
                SwingUtilities.invokeLater(() -> {
                    startBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    prefixField.setEnabled(true);
                    statusLabel.setText(String.format("Scan completed! Found %d responsive device(s).", results.size()));
                    updateImportButtonCount();
                });
            }
        }, scanExecutor);
    }

    private void stopScan() {
        if (activeScanFuture != null && !activeScanFuture.isDone()) {
            activeScanFuture.cancel(true);
        }
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        prefixField.setEnabled(true);
        statusLabel.setText("Scan cancelled by user.");
    }

    private void setAllSelected(boolean selected) {
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            tableModel.setValueAt(selected, r, 0);
        }
        updateImportButtonCount();
    }

    private void updateImportButtonCount() {
        int count = 0;
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            Boolean checked = (Boolean) tableModel.getValueAt(r, 0);
            if (Boolean.TRUE.equals(checked)) count++;
        }
        importBtn.setText(String.format("📥 Import Selected Devices (%d)", count));
        importBtn.setEnabled(count > 0);
    }

    private void importSelectedDevices() {
        int imported = 0;
        int skipped = 0;

        for (int r = 0; r < tableModel.getRowCount(); r++) {
            Boolean checked = (Boolean) tableModel.getValueAt(r, 0);
            if (!Boolean.TRUE.equals(checked)) continue;

            String ipRaw = (String) tableModel.getValueAt(r, 1);
            String cleanIp = ipRaw.replace(" (Exists)", "").trim();
            String hostName = (String) tableModel.getValueAt(r, 2);
            String mac = (String) tableModel.getValueAt(r, 3);
            String type = ((String) tableModel.getValueAt(r, 4)).toLowerCase();

            if (deviceDAO.deviceExistsByIp(cleanIp)) {
                skipped++;
                continue;
            }

            Device dev = new Device();
            dev.setDeviceName(hostName != null && !hostName.equals(cleanIp) ? hostName : "LAN-" + cleanIp);
            dev.setIpAddress(cleanIp);
            dev.setMacAddress(mac != null ? mac : "N/A");
            dev.setDeviceType(type != null ? type : "pc");
            dev.setLocation("Local Subnet");
            dev.setStatus("ONLINE");
            dev.setDescription("Discovered via Subnet Range Sweep");
            dev.setAddedBy(currentUser != null ? currentUser.getUserId() : 1);
            dev.setLastSeen(new Timestamp(System.currentTimeMillis()));

            if (deviceDAO.addDevice(dev)) {
                imported++;
            }
        }

        DeviceCache.getInstance().invalidate();

        if (currentUser != null && imported > 0) {
            auditLogDAO.log(new AuditLog(currentUser.getUserId(), "DEVICE_IMPORT",
                    String.format("Imported %d new devices from subnet sweep into inventory", imported)));
        }

        onImportSuccessCallbackTriggered = true;
        JOptionPane.showMessageDialog(this,
                String.format("Successfully imported %d device(s)!\nSkipped %d existing device(s).", imported, skipped),
                "Import Complete", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    public boolean hasImportedDevices() {
        return onImportSuccessCallbackTriggered;
    }
}
