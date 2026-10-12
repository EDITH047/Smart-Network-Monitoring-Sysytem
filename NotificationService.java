package com.networkmonitor.service;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;

public class NotificationService {
    private static final NotificationService instance = new NotificationService();
    private TrayIcon trayIcon;

    private NotificationService() {
        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                Image image = createDefaultIcon();
                trayIcon = new TrayIcon(image, "Smart Network Monitor");
                trayIcon.setImageAutoSize(true);
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("[NotificationService] TrayIcon could not be added: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("[NotificationService] SystemTray error: " + e.getMessage());
            }
        }
    }

    private Image createDefaultIcon() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new java.awt.Color(13, 110, 253));
        g2.fillOval(1, 1, 14, 14);
        g2.setColor(java.awt.Color.WHITE);
        g2.fillOval(5, 5, 6, 6);
        g2.dispose();
        return img;
    }

    public static NotificationService getInstance() {
        return instance;
    }

    public void showNotification(String title, String message, TrayIcon.MessageType type) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, type);
        }
    }
}
