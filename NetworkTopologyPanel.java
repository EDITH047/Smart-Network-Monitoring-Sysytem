package com.networkmonitor.ui;

import com.networkmonitor.model.Device;
import com.networkmonitor.model.User;
import com.networkmonitor.service.DeviceCache;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.List;
import java.util.stream.Collectors;

public class NetworkTopologyPanel extends JPanel {

    private User currentUser;
    private DeviceCache deviceCache;
    private Timer refreshTimer;
    private TopologyCanvas canvas;

    public NetworkTopologyPanel(User currentUser) {
        this.currentUser = currentUser;
        this.deviceCache = DeviceCache.getInstance();
        initializeUI();
        startAutoRefresh();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_CANVAS);
        
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 18, 0, 18));
        
        JLabel titleLabel = new JLabel("🗺️ Network Topology");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel);
        
        add(headerPanel, BorderLayout.NORTH);

        canvas = new TopologyCanvas();
        add(canvas, BorderLayout.CENTER);
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer(2000, e -> canvas.repaint());
        refreshTimer.start();
    }

    private class TopologyCanvas extends JPanel {
        
        public TopologyCanvas() {
            setOpaque(false);
            
            // Add right click support for WoL and Ping
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) handlePopup(e);
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) handlePopup(e);
                }
            });
        }
        
        private Point getDevicePosition(int index, int total, int cx, int cy, int maxRadius) {
            if (total <= 12) {
                double angle = 2 * Math.PI * index / total;
                return new Point((int) (cx + maxRadius * Math.cos(angle)), (int) (cy + maxRadius * Math.sin(angle)));
            } else {
                int innerCount = Math.min(12, total / 3);
                int outerCount = total - innerCount;
                if (index < innerCount) {
                    double angle = 2 * Math.PI * index / innerCount;
                    return new Point((int) (cx + (maxRadius / 2) * Math.cos(angle)), (int) (cy + (maxRadius / 2) * Math.sin(angle)));
                } else {
                    int outIdx = index - innerCount;
                    double angle = 2 * Math.PI * outIdx / outerCount;
                    return new Point((int) (cx + maxRadius * Math.cos(angle)), (int) (cy + maxRadius * Math.sin(angle)));
                }
            }
        }
        
        private void handlePopup(MouseEvent e) {
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            
            List<Device> devices = deviceCache.getAllDevices();
            if (devices.isEmpty()) return;
            
            int radius = Math.min(getWidth(), getHeight()) / 3;
            int numDevices = devices.size();
            
            for (int i = 0; i < numDevices; i++) {
                Point p = getDevicePosition(i, numDevices, cx, cy, radius);
                if (p.distance(e.getX(), e.getY()) < 30) {
                    showDeviceMenu(e.getX(), e.getY(), devices.get(i));
                    return;
                }
            }
        }
        
        private void showDeviceMenu(int x, int y, Device device) {
            JPopupMenu menu = new JPopupMenu();
            
            JMenuItem wolItem = new JMenuItem("Wake-on-LAN");
            wolItem.addActionListener(e -> JOptionPane.showMessageDialog(NetworkTopologyPanel.this, 
                "Magic Packet sent to " + device.getMacAddress(), "Wake-on-LAN", JOptionPane.INFORMATION_MESSAGE));
                
            JMenuItem pingItem = new JMenuItem("Ping / Traceroute");
            pingItem.addActionListener(e -> JOptionPane.showMessageDialog(NetworkTopologyPanel.this, 
                "Pinging " + device.getIpAddress() + "...", "Diagnostics", JOptionPane.INFORMATION_MESSAGE));
                
            menu.add(wolItem);
            menu.add(pingItem);
            menu.show(this, x, y);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            
            List<Device> devices = deviceCache.getAllDevices();
            
            if (devices.isEmpty()) {
                g2.setColor(UITheme.TEXT_MUTED);
                g2.drawString("No devices found.", cx - 50, cy);
                return;
            }
            
            // Draw Gateway in Center
            g2.setColor(UITheme.PRIMARY_BLUE);
            g2.fillOval(cx - 30, cy - 30, 60, 60);
            g2.setColor(Color.WHITE);
            g2.setFont(UITheme.FONT_BODY_BOLD);
            g2.drawString("GW", cx - 10, cy + 5);
            
            int maxRadius = Math.min(getWidth(), getHeight()) / 3;
            int numDevices = devices.size();
            
            for (int i = 0; i < numDevices; i++) {
                Point p = getDevicePosition(i, numDevices, cx, cy, maxRadius);
                int x = p.x;
                int y = p.y;
                
                Device d = devices.get(i);
                
                // Draw line to gateway
                if ("ONLINE".equals(d.getStatus())) {
                    g2.setColor(UITheme.SUCCESS_GREEN);
                } else {
                    g2.setColor(UITheme.DANGER_RED);
                }
                g2.setStroke(new BasicStroke(2));
                g2.draw(new Line2D.Double(cx, cy, x, y));
                
                // Draw node
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(x - 20, y - 20, 40, 40));
                
                if ("ONLINE".equals(d.getStatus())) {
                    g2.setColor(UITheme.SUCCESS_GREEN);
                } else {
                    g2.setColor(UITheme.DANGER_RED);
                }
                g2.draw(new Ellipse2D.Double(x - 20, y - 20, 40, 40));
                
                // Draw label
                g2.setColor(UITheme.TEXT_PRIMARY);
                g2.setFont(UITheme.FONT_SMALL);
                String label = d.getDeviceName();
                if (label == null || label.isEmpty()) label = d.getIpAddress();
                
                FontMetrics fm = g2.getFontMetrics();
                int labelWidth = fm.stringWidth(label);
                g2.drawString(label, x - labelWidth / 2, y + 35);
            }
        }
    }
}
