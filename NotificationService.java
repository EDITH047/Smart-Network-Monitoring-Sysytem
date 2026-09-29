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
                // Create a simple blank image for the icon if we don't have one
                Image image = Toolkit.getDefaultToolkit().createImage(new byte[0]); 
                trayIcon = new TrayIcon(image, "Smart Network Monitor");
                trayIcon.setImageAutoSize(true);
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("[NotificationService] TrayIcon could not be added.");
            }
        }
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
