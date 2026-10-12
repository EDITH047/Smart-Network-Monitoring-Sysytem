package com.networkmonitor.ui;

import com.networkmonitor.model.Device;
import com.networkmonitor.model.User;
import com.networkmonitor.service.PortScanner;
import com.networkmonitor.service.PortScanner.PortScanResult;
import com.networkmonitor.util.NetworkAdapter;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NetworkDiagnosticsDialog — Enterprise diagnostics workbench providing:
 * 1. Multi-threaded TCP Port Scanner with banner analyzer
 * 2. Asynchronous Route Hop Tracer (Traceroute)
 * 3. Continuous Ping with min/avg/max telemetry
 */
public class NetworkDiagnosticsDialog extends JDialog {

    private final User currentUser;
    private final String initialIp;
    private final String initialName;

    private JTextField targetIpField;
    private JComboBox<String> profileCombo;
    private JTextField customPortsField;
    private JButton scanPortsBtn;
    private JButton stopPortsBtn;
    private JProgressBar portProgressBar;
    private JLabel portStatusLabel;
    private JTable portTable;
    private DefaultTableModel portTableModel;

    private JButton traceBtn;
    private JButton stopTraceBtn;
    private JTable traceTable;
    private DefaultTableModel traceTableModel;
    private JLabel traceStatusLabel;

    private JButton pingBtn;
    private JButton stopPingBtn;
    private JTextArea pingLogArea;
    private JLabel pingStatsLabel;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private Future<?> activePortScan;
    private Process activeTraceProcess;
    private Timer pingTimer;

    // Ping telemetry
    private int pingSent = 0;
    private int pingReceived = 0;
    private long minLatency = Long.MAX_VALUE;
    private long maxLatency = 0;
    private long totalLatency = 0;

    public NetworkDiagnosticsDialog(Window parent, User currentUser, Device device) {
        this(parent, currentUser, device != null ? device.getIpAddress() : "127.0.0.1",
                device != null ? device.getDeviceName() : "Target Host");
    }

    public NetworkDiagnosticsDialog(Window parent, User currentUser, String ip, String name) {
        super(parent, "🩺 Network Diagnostics Workbench — " + name + " (" + ip + ")", ModalityType.MODELESS);
        this.currentUser = currentUser;
        this.initialIp = ip != null ? ip : "127.0.0.1";
        this.initialName = name != null ? name : "Host";

        initializeUI();
        setSize(920, 640);
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        JPanel contentPane = new JPanel(new BorderLayout(12, 12));
        contentPane.setBackground(UITheme.BG_CANVAS);
        contentPane.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // Top Target Bar
        JPanel targetBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        targetBar.setOpaque(false);

        JLabel hostLbl = new JLabel("Target IP / Host:");
        hostLbl.setFont(UITheme.FONT_BODY_BOLD);
        targetBar.add(hostLbl);

        targetIpField = new JTextField(initialIp, 14);
        targetIpField.setFont(UITheme.FONT_BODY);
        targetBar.add(targetIpField);

        JLabel nameBadge = new JLabel("  🏷️ " + initialName + "  ");
        nameBadge.setFont(UITheme.FONT_SMALL);
        nameBadge.setOpaque(true);
        nameBadge.setBackground(UITheme.CARD_BG);
        nameBadge.setForeground(UITheme.PRIMARY_BLUE);
        nameBadge.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        targetBar.add(nameBadge);

        contentPane.add(targetBar, BorderLayout.NORTH);

        // Center Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        UITheme.styleTabbedPane(tabbedPane);

        tabbedPane.addTab("🔌 Port Scanner", createPortScannerPanel());
        tabbedPane.addTab("🗺️ Hop Traceroute", createTraceroutePanel());
        tabbedPane.addTab("📡 Continuous Ping", createContinuousPingPanel());

        contentPane.add(tabbedPane, BorderLayout.CENTER);

        // Close hook to cleanup workers
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cleanup();
            }
        });

        setContentPane(contentPane);
    }

    private JPanel createPortScannerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        controls.setOpaque(false);

        controls.add(new JLabel("Port Profile:"));
        profileCombo = new JComboBox<>(new String[]{
            "Common Services (21 Ports)",
            "Web Services (HTTP/HTTPS)",
            "Database Engines",
            "Admin & Remote Access",
            "Custom Ports / Range"
        });
        controls.add(profileCombo);

        customPortsField = new JTextField("80, 443, 8080", 12);
        customPortsField.setEnabled(false);
        controls.add(customPortsField);

        profileCombo.addActionListener(e -> {
            boolean isCustom = profileCombo.getSelectedIndex() == 4;
            customPortsField.setEnabled(isCustom);
        });

        scanPortsBtn = new JButton("🚀 Scan Ports");
        UITheme.stylePrimaryButton(scanPortsBtn);
        scanPortsBtn.addActionListener(e -> startPortScan());
        controls.add(scanPortsBtn);

        stopPortsBtn = new JButton("🛑 Stop");
        UITheme.styleDangerButton(stopPortsBtn);
        stopPortsBtn.setEnabled(false);
        stopPortsBtn.addActionListener(e -> stopPortScan());
        controls.add(stopPortsBtn);

        JPanel topContainer = new JPanel(new BorderLayout(6, 6));
        topContainer.setOpaque(false);
        topContainer.add(controls, BorderLayout.NORTH);

        portProgressBar = new JProgressBar();
        portProgressBar.setStringPainted(true);
        portProgressBar.setForeground(UITheme.PRIMARY_BLUE);
        portStatusLabel = new JLabel("Select profile and click 'Scan Ports'");
        portStatusLabel.setFont(UITheme.FONT_SMALL);
        portStatusLabel.setForeground(UITheme.TEXT_MUTED);

        JPanel progBarPanel = new JPanel(new BorderLayout(4, 2));
        progBarPanel.setOpaque(false);
        progBarPanel.add(portProgressBar, BorderLayout.NORTH);
        progBarPanel.add(portStatusLabel, BorderLayout.SOUTH);
        topContainer.add(progBarPanel, BorderLayout.SOUTH);

        panel.add(topContainer, BorderLayout.NORTH);

        // Table
        portTableModel = new DefaultTableModel(new String[]{
            "Port", "Service", "Status", "Latency", "Banner / Info"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        portTable = new JTable(portTableModel);
        UITheme.styleTable(portTable);
        portTable.getColumnModel().getColumn(0).setMaxWidth(80);
        portTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        portTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        portTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        portTable.getColumnModel().getColumn(4).setPreferredWidth(260);

        portTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, val, sel, foc, row, col);
                String text = val != null ? val.toString() : "";
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(UITheme.FONT_SMALL_BOLD);
                if ("OPEN".equalsIgnoreCase(text)) {
                    lbl.setForeground(UITheme.SUCCESS_GREEN);
                    lbl.setText("● OPEN");
                } else if ("CLOSED".equalsIgnoreCase(text)) {
                    lbl.setForeground(UITheme.TEXT_MUTED);
                    lbl.setText("○ CLOSED");
                } else {
                    lbl.setForeground(UITheme.WARNING_ORANGE);
                    lbl.setText("▲ FILTERED");
                }
                return lbl;
            }
        });

        JScrollPane scrollPane = new JScrollPane(portTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void startPortScan() {
        String targetIp = targetIpField.getText().trim();
        if (targetIp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Target IP cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int[] portsToScan;
        int profileIdx = profileCombo.getSelectedIndex();
        if (profileIdx == 0) portsToScan = PortScanner.PRESET_COMMON;
        else if (profileIdx == 1) portsToScan = PortScanner.PRESET_WEB;
        else if (profileIdx == 2) portsToScan = PortScanner.PRESET_DATABASE;
        else if (profileIdx == 3) portsToScan = PortScanner.PRESET_ADMIN;
        else {
            portsToScan = parseCustomPorts(customPortsField.getText().trim());
            if (portsToScan.length == 0) {
                JOptionPane.showMessageDialog(this, "Please enter valid comma-separated ports or range (e.g. 80, 443, 8000-8010).", "Invalid Ports", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        portTableModel.setRowCount(0);
        scanPortsBtn.setEnabled(false);
        stopPortsBtn.setEnabled(true);
        portProgressBar.setMaximum(portsToScan.length);
        portProgressBar.setValue(0);
        portStatusLabel.setText(String.format("Scanning %d ports on %s...", portsToScan.length, targetIp));

        activePortScan = PortScanner.scanTargetAsync(targetIp, portsToScan, 450, new PortScanner.PortScanListener() {
            @Override
            public void onPortScanned(PortScanResult res) {
                SwingUtilities.invokeLater(() -> {
                    // Always show OPEN and FILTERED ports, show CLOSED only if less than 50 ports
                    if ("OPEN".equals(res.getStatus()) || !"CLOSED".equals(res.getStatus()) || portsToScan.length <= 50) {
                        portTableModel.addRow(new Object[]{
                            res.getPort(),
                            res.getServiceName(),
                            res.getStatus(),
                            res.getResponseTimeMs() + " ms",
                            res.getBanner()
                        });
                    }
                });
            }

            @Override
            public void onProgress(int completed, int total) {
                SwingUtilities.invokeLater(() -> {
                    portProgressBar.setValue(completed);
                    portStatusLabel.setText(String.format("Scanning %s... %d / %d completed", targetIp, completed, total));
                });
            }

            @Override
            public void onComplete(List<PortScanResult> results) {
                SwingUtilities.invokeLater(() -> {
                    scanPortsBtn.setEnabled(true);
                    stopPortsBtn.setEnabled(false);
                    long openCount = results.stream().filter(r -> "OPEN".equals(r.getStatus())).count();
                    portStatusLabel.setText(String.format("Scan finished! Found %d OPEN port(s) out of %d probed.", openCount, results.size()));
                });
            }
        }, executorService);
    }

    private void stopPortScan() {
        if (activePortScan != null && !activePortScan.isDone()) {
            activePortScan.cancel(true);
        }
        scanPortsBtn.setEnabled(true);
        stopPortsBtn.setEnabled(false);
        portStatusLabel.setText("Port scan stopped by user.");
    }

    private int[] parseCustomPorts(String str) {
        try {
            List<Integer> list = new ArrayList<>();
            String[] tokens = str.split("[,;\\s]+");
            for (String tok : tokens) {
                if (tok.contains("-")) {
                    String[] bounds = tok.split("-");
                    int start = Integer.parseInt(bounds[0].trim());
                    int end = Integer.parseInt(bounds[1].trim());
                    for (int p = Math.min(start, end); p <= Math.max(start, end) && p <= 65535; p++) {
                        list.add(p);
                    }
                } else if (!tok.trim().isEmpty()) {
                    int p = Integer.parseInt(tok.trim());
                    if (p >= 1 && p <= 65535) list.add(p);
                }
            }
            return list.stream().mapToInt(i -> i).toArray();
        } catch (Exception e) {
            return new int[0];
        }
    }

    private JPanel createTraceroutePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topBar.setOpaque(false);

        traceBtn = new JButton("🗺️ Trace Route");
        UITheme.stylePrimaryButton(traceBtn);
        traceBtn.addActionListener(e -> startTraceroute());
        topBar.add(traceBtn);

        stopTraceBtn = new JButton("🛑 Stop");
        UITheme.styleDangerButton(stopTraceBtn);
        stopTraceBtn.setEnabled(false);
        stopTraceBtn.addActionListener(e -> stopTraceroute());
        topBar.add(stopTraceBtn);

        traceStatusLabel = new JLabel("Click 'Trace Route' to discover hop latencies.");
        traceStatusLabel.setFont(UITheme.FONT_SMALL);
        traceStatusLabel.setForeground(UITheme.TEXT_MUTED);
        topBar.add(traceStatusLabel);

        panel.add(topBar, BorderLayout.NORTH);

        traceTableModel = new DefaultTableModel(new String[]{
            "Hop #", "Hop IP / Host", "RTT 1", "RTT 2", "RTT 3", "Status"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        traceTable = new JTable(traceTableModel);
        UITheme.styleTable(traceTable);
        traceTable.getColumnModel().getColumn(0).setMaxWidth(60);
        traceTable.getColumnModel().getColumn(1).setPreferredWidth(220);

        JScrollPane scrollPane = new JScrollPane(traceTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void startTraceroute() {
        String targetIp = targetIpField.getText().trim();
        if (targetIp.isEmpty()) return;

        traceTableModel.setRowCount(0);
        traceBtn.setEnabled(false);
        stopTraceBtn.setEnabled(true);
        traceStatusLabel.setText("Tracing route to " + targetIp + "... Please wait.");

        executorService.submit(() -> {
            try {
                boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
                ProcessBuilder pb = isWindows ?
                        new ProcessBuilder("tracert", "-d", "-h", "20", "-w", "500", targetIp) :
                        new ProcessBuilder("traceroute", "-n", "-m", "20", "-w", "1", targetIp);
                pb.redirectErrorStream(true);
                activeTraceProcess = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(activeTraceProcess.getInputStream()))) {
                    String line;
                    Pattern winPattern = Pattern.compile("^\\s*(\\d+)\\s+([<\\d\\*\\s\\w]+ms|\\*)\\s+([<\\d\\*\\s\\w]+ms|\\*)\\s+([<\\d\\*\\s\\w]+ms|\\*)\\s+([0-9a-fA-F\\.:]+|Request timed out.*)");
                    while ((line = reader.readLine()) != null) {
                        final String finalLine = line;
                        Matcher m = winPattern.matcher(line);
                        if (m.find()) {
                            String hop = m.group(1).trim();
                            String rtt1 = m.group(2).trim();
                            String rtt2 = m.group(3).trim();
                            String rtt3 = m.group(4).trim();
                            String ip = m.group(5).trim();
                            boolean timeout = ip.toLowerCase().contains("timed out") || "*".equals(rtt1);
                            SwingUtilities.invokeLater(() -> traceTableModel.addRow(new Object[]{
                                hop, timeout ? "Timed Out (*)" : ip, rtt1, rtt2, rtt3, timeout ? "NO RESPONSE" : "ONLINE"
                            }));
                        } else if (line.matches("^\\s*\\d+\\s+.*")) {
                            // Generic hop line
                            String[] parts = line.trim().split("\\s+");
                            if (parts.length >= 2) {
                                SwingUtilities.invokeLater(() -> traceTableModel.addRow(new Object[]{
                                    parts[0], parts[1], parts.length > 2 ? parts[2] : "-", parts.length > 3 ? parts[3] : "-", parts.length > 4 ? parts[4] : "-", "REACHED"
                                }));
                            }
                        }
                    }
                }
                activeTraceProcess.waitFor();
            } catch (Exception ignored) {
            } finally {
                SwingUtilities.invokeLater(() -> {
                    traceBtn.setEnabled(true);
                    stopTraceBtn.setEnabled(false);
                    traceStatusLabel.setText("Route trace complete.");
                });
            }
        });
    }

    private void stopTraceroute() {
        if (activeTraceProcess != null) {
            activeTraceProcess.destroyForcibly();
        }
        traceBtn.setEnabled(true);
        stopTraceBtn.setEnabled(false);
        traceStatusLabel.setText("Trace aborted.");
    }

    private JPanel createContinuousPingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topBar.setOpaque(false);

        pingBtn = new JButton("📡 Start Continuous Ping");
        UITheme.stylePrimaryButton(pingBtn);
        pingBtn.addActionListener(e -> startContinuousPing());
        topBar.add(pingBtn);

        stopPingBtn = new JButton("🛑 Stop");
        UITheme.styleDangerButton(stopPingBtn);
        stopPingBtn.setEnabled(false);
        stopPingBtn.addActionListener(e -> stopContinuousPing());
        topBar.add(stopPingBtn);

        pingStatsLabel = new JLabel("Sent: 0 | Received: 0 | Loss: 0.0% | Min: - | Avg: - | Max: -");
        pingStatsLabel.setFont(UITheme.FONT_BODY_BOLD);
        pingStatsLabel.setForeground(UITheme.TEXT_PRIMARY);
        topBar.add(pingStatsLabel);

        panel.add(topBar, BorderLayout.NORTH);

        pingLogArea = new JTextArea();
        pingLogArea.setEditable(false);
        pingLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        pingLogArea.setBackground(UITheme.BG_DARK_HEADER);
        pingLogArea.setForeground(new Color(226, 232, 240));

        JScrollPane scrollPane = new JScrollPane(pingLogArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void startContinuousPing() {
        String targetIp = targetIpField.getText().trim();
        if (targetIp.isEmpty()) return;

        pingSent = 0;
        pingReceived = 0;
        minLatency = Long.MAX_VALUE;
        maxLatency = 0;
        totalLatency = 0;
        pingLogArea.setText("");

        pingBtn.setEnabled(false);
        stopPingBtn.setEnabled(true);

        pingTimer = new Timer(1000, e -> executorService.submit(() -> {
            pingSent++;
            long latency = NetworkAdapter.getLatencyMs(targetIp);
            SwingUtilities.invokeLater(() -> {
                if (latency >= 0) {
                    pingReceived++;
                    minLatency = Math.min(minLatency, latency);
                    maxLatency = Math.max(maxLatency, latency);
                    totalLatency += latency;
                    long avg = totalLatency / pingReceived;
                    pingLogArea.append(String.format("[%tT] Reply from %s: time=%dms\n", System.currentTimeMillis(), targetIp, latency));
                    updatePingStats(avg);
                } else {
                    pingLogArea.append(String.format("[%tT] Request timed out to %s\n", System.currentTimeMillis(), targetIp));
                    updatePingStats(pingReceived > 0 ? totalLatency / pingReceived : 0);
                }
                pingLogArea.setCaretPosition(pingLogArea.getDocument().getLength());
            });
        }));
        pingTimer.start();
    }

    private void updatePingStats(long avg) {
        double loss = pingSent > 0 ? ((pingSent - pingReceived) * 100.0 / pingSent) : 0;
        pingStatsLabel.setText(String.format("Sent: %d | Recv: %d | Loss: %.1f%% | Min: %dms | Avg: %dms | Max: %dms",
                pingSent, pingReceived, loss, minLatency == Long.MAX_VALUE ? 0 : minLatency, avg, maxLatency));
    }

    private void stopContinuousPing() {
        if (pingTimer != null) {
            pingTimer.stop();
        }
        pingBtn.setEnabled(true);
        stopPingBtn.setEnabled(false);
    }

    private void cleanup() {
        stopPortScan();
        stopTraceroute();
        stopContinuousPing();
        executorService.shutdownNow();
    }
}
