package com.networkmonitor.ui;

import com.networkmonitor.dao.AuditLogDAO;
import com.networkmonitor.dao.FirewallRuleDAO;
import com.networkmonitor.model.AuditLog;
import com.networkmonitor.model.Device;
import com.networkmonitor.model.FirewallRule;
import com.networkmonitor.model.User;
import com.networkmonitor.service.DeviceCache;
import com.networkmonitor.service.FirewallService;
import com.networkmonitor.util.NetworkAdapter;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * NetworkTopologyPanel — Dynamic, interactive network map visualizer featuring:
 * - Draggable nodes with mouse physics
 * - Real-time animated traffic particle pulses along connections
 * - Rich hover inspection telemetry cards
 * - Node context menus with 1-click port scan, ping diagnostics, and firewall isolation
 */
public class NetworkTopologyPanel extends JPanel {

    private final User currentUser;
    private final DeviceCache deviceCache;
    private final FirewallRuleDAO firewallRuleDAO = new FirewallRuleDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private javax.swing.Timer animationTimer;
    private TopologyCanvas canvas;

    public NetworkTopologyPanel(User currentUser) {
        this.currentUser = currentUser;
        this.deviceCache = DeviceCache.getInstance();
        initializeUI();
        startAnimation();
        UITheme.addThemeChangeListener(canvas::repaint);
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_CANVAS);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 8, 18));

        JPanel titleContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleContainer.setOpaque(false);
        JLabel titleLabel = new JLabel("🗺️ Network Topology Visualizer");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        titleContainer.add(titleLabel);

        JLabel tipLabel = new JLabel("💡 Click & drag nodes to organize • Right-click for Diagnostics & Firewall controls");
        tipLabel.setFont(UITheme.FONT_SMALL);
        tipLabel.setForeground(UITheme.TEXT_MUTED);
        titleContainer.add(tipLabel);

        headerPanel.add(titleContainer, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton resetLayoutBtn = new JButton("🔄 Reset Layout");
        UITheme.styleNeutralButton(resetLayoutBtn);
        resetLayoutBtn.addActionListener(e -> {
            canvas.resetNodePositions();
            canvas.repaint();
        });
        btnPanel.add(resetLayoutBtn);

        headerPanel.add(btnPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        canvas = new TopologyCanvas();
        add(canvas, BorderLayout.CENTER);
    }

    private void startAnimation() {
        // 35ms timer (~28 FPS) for smooth traffic pulse rendering
        animationTimer = new javax.swing.Timer(35, e -> {
            canvas.advanceAnimation();
            canvas.repaint();
        });
        animationTimer.start();
    }

    private class TopologyCanvas extends JPanel {

        private final Map<Integer, Point> customPositions = new HashMap<>();
        private Point gatewayPosition = null;

        private Integer draggedDeviceId = null; // -1 represents Gateway
        private Point dragOffset = new Point(0, 0);

        private Integer hoveredDeviceId = null;
        private Point mousePos = new Point(-100, -100);

        private float trafficPhase = 0.0f;

        public TopologyCanvas() {
            setOpaque(false);

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        handlePopup(e);
                        return;
                    }

                    Point click = e.getPoint();
                    Point gwPos = getGatewayPos();
                    if (gwPos.distance(click) <= 30) {
                        draggedDeviceId = -1;
                        dragOffset = new Point(click.x - gwPos.x, click.y - gwPos.y);
                        return;
                    }

                    List<Device> devices = deviceCache.getAllDevices();
                    for (Device d : devices) {
                        Point dp = getNodePos(d);
                        if (dp.distance(click) <= 25) {
                            draggedDeviceId = d.getDeviceId();
                            dragOffset = new Point(click.x - dp.x, click.y - dp.y);
                            return;
                        }
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggedDeviceId != null) {
                        Point p = new Point(e.getX() - dragOffset.x, e.getY() - dragOffset.y);
                        if (draggedDeviceId == -1) {
                            gatewayPosition = p;
                        } else {
                            customPositions.put(draggedDeviceId, p);
                        }
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        handlePopup(e);
                    }
                    draggedDeviceId = null;
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    mousePos = e.getPoint();
                    Integer prevHover = hoveredDeviceId;
                    hoveredDeviceId = null;

                    Point gwPos = getGatewayPos();
                    if (gwPos.distance(mousePos) <= 30) {
                        hoveredDeviceId = -1;
                    } else {
                        List<Device> devices = deviceCache.getAllDevices();
                        for (Device d : devices) {
                            Point dp = getNodePos(d);
                            if (dp.distance(mousePos) <= 25) {
                                hoveredDeviceId = d.getDeviceId();
                                break;
                            }
                        }
                    }

                    if (!Objects.equals(prevHover, hoveredDeviceId)) {
                        repaint();
                    }
                }
            };

            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        public void advanceAnimation() {
            trafficPhase = (trafficPhase + 0.03f) % 1.0f;
        }

        public void resetNodePositions() {
            customPositions.clear();
            gatewayPosition = null;
        }

        private Point getGatewayPos() {
            if (gatewayPosition != null) return gatewayPosition;
            return new Point(getWidth() / 2, getHeight() / 2);
        }

        private Point getNodePos(Device device) {
            Point p = customPositions.get(device.getDeviceId());
            if (p != null) return p;

            // Default radial distribution
            List<Device> devices = deviceCache.getAllDevices();
            int idx = devices.indexOf(device);
            if (idx == -1) idx = 0;
            int total = Math.max(1, devices.size());

            Point gw = getGatewayPos();
            int maxRadius = Math.max(120, Math.min(getWidth(), getHeight()) / 3);

            if (total <= 12) {
                double angle = 2 * Math.PI * idx / total;
                return new Point((int) (gw.x + maxRadius * Math.cos(angle)), (int) (gw.y + maxRadius * Math.sin(angle)));
            } else {
                int innerCount = Math.min(12, total / 3);
                int outerCount = total - innerCount;
                if (idx < innerCount) {
                    double angle = 2 * Math.PI * idx / innerCount;
                    return new Point((int) (gw.x + (maxRadius / 2) * Math.cos(angle)), (int) (gw.y + (maxRadius / 2) * Math.sin(angle)));
                } else {
                    int outIdx = idx - innerCount;
                    double angle = 2 * Math.PI * outIdx / outerCount;
                    return new Point((int) (gw.x + maxRadius * Math.cos(angle)), (int) (gw.y + maxRadius * Math.sin(angle)));
                }
            }
        }

        private void handlePopup(MouseEvent e) {
            Point p = e.getPoint();
            Point gwPos = getGatewayPos();

            if (gwPos.distance(p) <= 30) {
                showGatewayMenu(e.getX(), e.getY());
                return;
            }

            List<Device> devices = deviceCache.getAllDevices();
            for (Device d : devices) {
                Point dp = getNodePos(d);
                if (dp.distance(p) <= 25) {
                    showDeviceMenu(e.getX(), e.getY(), d);
                    return;
                }
            }
        }

        private void showGatewayMenu(int x, int y) {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem diagItem = new JMenuItem("🩺 Ping Gateway & Diagnostics");
            diagItem.addActionListener(e -> {
                NetworkDiagnosticsDialog dlg = new NetworkDiagnosticsDialog(
                        SwingUtilities.getWindowAncestor(NetworkTopologyPanel.this),
                        currentUser, "192.168.1.1", "Default Gateway");
                dlg.setVisible(true);
            });
            menu.add(diagItem);
            menu.show(this, x, y);
        }

        private void showDeviceMenu(int x, int y, Device device) {
            JPopupMenu menu = new JPopupMenu();

            JMenuItem diagItem = new JMenuItem("🩺 Diagnostics & Port Scanner");
            diagItem.addActionListener(e -> {
                NetworkDiagnosticsDialog dlg = new NetworkDiagnosticsDialog(
                        SwingUtilities.getWindowAncestor(NetworkTopologyPanel.this),
                        currentUser, device);
                dlg.setVisible(true);
            });
            menu.add(diagItem);

            JMenuItem wolItem = new JMenuItem("⚡ Send Wake-on-LAN");
            wolItem.addActionListener(e -> {
                String mac = device.getMacAddress();
                if (mac == null || mac.trim().isEmpty() || "N/A".equalsIgnoreCase(mac.trim())) {
                    JOptionPane.showMessageDialog(NetworkTopologyPanel.this,
                            "Cannot send Wake-on-LAN: No MAC registered for " + device.getDeviceName(),
                            "Wake-on-LAN", JOptionPane.WARNING_MESSAGE);
                } else {
                    boolean ok = sendWakeOnLan(mac.trim());
                    JOptionPane.showMessageDialog(NetworkTopologyPanel.this,
                            ok ? "Magic packet broadcasted to " + mac : "Failed to broadcast magic packet.",
                            "Wake-on-LAN", ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                }
            });
            menu.add(wolItem);

            menu.addSeparator();

            JMenuItem blockItem = new JMenuItem("🛡️ Block Device IP in Firewall");
            blockItem.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(NetworkTopologyPanel.this,
                        "Are you sure you want to block all traffic for " + device.getDeviceName() + " (" + device.getIpAddress() + ")?",
                        "Block Device", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    FirewallRule rule = new FirewallRule();
                    rule.setRuleName("Isolate " + device.getDeviceName());
                    rule.setSourceIp(device.getIpAddress());
                    rule.setDestinationIp("*");
                    rule.setPort(-1);
                    rule.setProtocol("ALL");
                    rule.setAction("DROP");
                    rule.setIsActive(true);
                    rule.setCreatedBy(currentUser != null ? currentUser.getUserId() : 1);

                    if (firewallRuleDAO.addRule(rule)) {
                        FirewallService.getInstance().reloadRules();
                        if (currentUser != null) {
                            auditLogDAO.log(new AuditLog(currentUser.getUserId(), "FIREWALL_BLOCK",
                                    "Added DROP firewall rule for " + device.getIpAddress()));
                        }
                        JOptionPane.showMessageDialog(NetworkTopologyPanel.this,
                                "Firewall DROP rule successfully enforced for " + device.getIpAddress(),
                                "Device Isolated", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(NetworkTopologyPanel.this,
                                "Failed to create firewall rule.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            menu.add(blockItem);

            menu.show(this, x, y);
        }

        private boolean sendWakeOnLan(String macStr) {
            try {
                String cleanMac = macStr.replaceAll("[:-]", "");
                if (cleanMac.length() != 12) return false;
                byte[] macBytes = new byte[6];
                for (int i = 0; i < 6; i++) {
                    macBytes[i] = (byte) Integer.parseInt(cleanMac.substring(i * 2, i * 2 + 2), 16);
                }
                byte[] bytes = new byte[6 + 16 * macBytes.length];
                for (int i = 0; i < 6; i++) bytes[i] = (byte) 0xff;
                for (int i = 6; i < bytes.length; i += macBytes.length) {
                    System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
                }
                java.net.InetAddress address = java.net.InetAddress.getByName("255.255.255.255");
                java.net.DatagramPacket packet = new java.net.DatagramPacket(bytes, bytes.length, address, 9);
                try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
                    socket.setBroadcast(true);
                    socket.send(packet);
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Point gw = getGatewayPos();
            List<Device> devices = deviceCache.getAllDevices();

            if (devices.isEmpty()) {
                g2.setColor(UITheme.TEXT_MUTED);
                g2.setFont(UITheme.FONT_BODY);
                g2.drawString("No network devices registered. Use 'Subnet Sweep' in Devices to discover nodes.",
                        getWidth() / 2 - 180, getHeight() / 2);
                return;
            }

            // 1. Draw connection lines and animated traffic pulse packets
            for (int i = 0; i < devices.size(); i++) {
                Device d = devices.get(i);
                Point dp = getNodePos(d);
                boolean online = "ONLINE".equalsIgnoreCase(d.getStatus());

                // Base connection line
                g2.setStroke(new BasicStroke(online ? 2.0f : 1.5f));
                g2.setColor(online ? new Color(16, 185, 129, 140) : new Color(239, 68, 68, 80));
                g2.draw(new Line2D.Double(gw.x, gw.y, dp.x, dp.y));

                // Traffic particle animations along active online links
                if (online) {
                    float offset1 = (trafficPhase + (float) i * 0.15f) % 1.0f;
                    float offset2 = (trafficPhase + (float) i * 0.15f + 0.5f) % 1.0f;

                    drawPulseParticle(g2, gw, dp, offset1);
                    drawPulseParticle(g2, gw, dp, offset2);
                }
            }

            // 2. Draw Gateway Node in Center
            boolean gwHovered = hoveredDeviceId != null && hoveredDeviceId == -1;
            if (gwHovered) {
                g2.setColor(new Color(59, 130, 246, 80));
                g2.fillOval(gw.x - 38, gw.y - 38, 76, 76);
            }
            g2.setColor(UITheme.PRIMARY_BLUE);
            g2.fillOval(gw.x - 28, gw.y - 28, 56, 56);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawOval(gw.x - 28, gw.y - 28, 56, 56);

            g2.setFont(UITheme.FONT_BODY_BOLD);
            g2.drawString("GW", gw.x - 12, gw.y + 6);

            g2.setColor(UITheme.TEXT_PRIMARY);
            g2.setFont(UITheme.FONT_SMALL_BOLD);
            g2.drawString("Gateway Router", gw.x - 42, gw.y + 45);

            // 3. Draw Device Nodes
            Device hoveredDevice = null;
            Point hoveredNodePos = null;

            for (Device d : devices) {
                Point dp = getNodePos(d);
                boolean isOnline = "ONLINE".equalsIgnoreCase(d.getStatus());
                boolean isHovered = hoveredDeviceId != null && hoveredDeviceId == d.getDeviceId();

                if (isHovered) {
                    hoveredDevice = d;
                    hoveredNodePos = dp;
                    g2.setColor(new Color(59, 130, 246, 70));
                    g2.fillOval(dp.x - 28, dp.y - 28, 56, 56);
                }

                // Inner Circle
                g2.setColor(UITheme.CARD_BG);
                g2.fill(new Ellipse2D.Double(dp.x - 20, dp.y - 20, 40, 40));

                // Border
                g2.setColor(isOnline ? UITheme.SUCCESS_GREEN : UITheme.DANGER_RED);
                g2.setStroke(new BasicStroke(isHovered ? 3.0f : 2.0f));
                g2.draw(new Ellipse2D.Double(dp.x - 20, dp.y - 20, 40, 40));

                // Device Type Icon / Letter
                g2.setColor(UITheme.TEXT_PRIMARY);
                g2.setFont(UITheme.FONT_SMALL_BOLD);
                String badge = getDeviceTypeBadge(d.getDeviceType());
                FontMetrics bfm = g2.getFontMetrics();
                g2.drawString(badge, dp.x - bfm.stringWidth(badge) / 2, dp.y + 5);

                // Label
                g2.setColor(UITheme.TEXT_PRIMARY);
                g2.setFont(UITheme.FONT_SMALL);
                String name = d.getDeviceName() != null ? d.getDeviceName() : d.getIpAddress();
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(name);
                g2.drawString(name, dp.x - textW / 2, dp.y + 34);
            }

            // 4. Render Floating Inspection Card if a node is hovered
            if (hoveredDevice != null && hoveredNodePos != null) {
                renderHoverCard(g2, hoveredDevice, hoveredNodePos);
            } else if (gwHovered) {
                renderGatewayCard(g2, gw);
            }
        }

        private void drawPulseParticle(Graphics2D g2, Point from, Point to, float fraction) {
            double x = from.x + (to.x - from.x) * fraction;
            double y = from.y + (to.y - from.y) * fraction;

            g2.setColor(new Color(56, 189, 248, 220)); // Glowing Cyan/Sky
            g2.fillOval((int) (x - 4), (int) (y - 4), 8, 8);
        }

        private String getDeviceTypeBadge(String type) {
            if (type == null) return "PC";
            switch (type.toLowerCase()) {
                case "router": return "RT";
                case "switch": return "SW";
                case "printer": return "PR";
                case "server": return "SRV";
                case "mobile": case "phone": return "MOB";
                default: return "PC";
            }
        }

        private void renderHoverCard(Graphics2D g2, Device d, Point pos) {
            int cardW = 200;
            int cardH = 96;
            int cardX = Math.min(pos.x + 24, getWidth() - cardW - 16);
            int cardY = Math.max(16, Math.min(pos.y - 40, getHeight() - cardH - 16));

            boolean dark = UITheme.isDarkMode();
            g2.setColor(dark ? new Color(15, 23, 42, 240) : new Color(255, 255, 255, 245));
            g2.fill(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 12, 12));
            g2.setColor(UITheme.BORDER_DARK);
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 12, 12));

            // Content
            g2.setColor(dark ? Color.WHITE : new Color(15, 23, 42));
            g2.setFont(UITheme.FONT_BODY_BOLD);
            g2.drawString(d.getDeviceName(), cardX + 12, cardY + 22);

            g2.setFont(UITheme.FONT_SMALL);
            g2.setColor(dark ? new Color(203, 213, 225) : new Color(71, 85, 105));
            g2.drawString("IP: " + d.getIpAddress(), cardX + 12, cardY + 40);
            g2.drawString("MAC: " + d.getMacAddress(), cardX + 12, cardY + 56);
            g2.drawString("Type: " + d.getDeviceType().toUpperCase(), cardX + 12, cardY + 72);

            boolean isOnline = "ONLINE".equalsIgnoreCase(d.getStatus());
            g2.setColor(isOnline ? UITheme.SUCCESS_GREEN : UITheme.DANGER_RED);
            g2.drawString("● " + (isOnline ? "ONLINE" : "OFFLINE"), cardX + 12, cardY + 88);
        }

        private void renderGatewayCard(Graphics2D g2, Point pos) {
            int cardW = 190;
            int cardH = 75;
            int cardX = Math.min(pos.x + 28, getWidth() - cardW - 16);
            int cardY = Math.max(16, pos.y - 35);

            boolean dark = UITheme.isDarkMode();
            g2.setColor(dark ? new Color(15, 23, 42, 240) : new Color(255, 255, 255, 245));
            g2.fill(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 12, 12));
            g2.setColor(UITheme.PRIMARY_BLUE);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 12, 12));

            g2.setColor(dark ? Color.WHITE : new Color(15, 23, 42));
            g2.setFont(UITheme.FONT_BODY_BOLD);
            g2.drawString("Default Gateway", cardX + 12, cardY + 22);

            g2.setFont(UITheme.FONT_SMALL);
            g2.setColor(dark ? new Color(203, 213, 225) : new Color(71, 85, 105));
            g2.drawString("Central Network Router", cardX + 12, cardY + 42);
            g2.setColor(UITheme.SUCCESS_GREEN);
            g2.drawString("● ONLINE & ROUTING", cardX + 12, cardY + 62);
        }
    }
}
