package com.networkmonitor.util;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * UITheme - Centralized theme and UI styling helper for Swing components
 * Fixes Swing button background rendering issues across Windows/Linux System Look-and-Feels
 */
public class UITheme {

    // Primary Brand Colors
    public static final Color PRIMARY_BLUE = new Color(37, 99, 235);     // #2563EB - Primary Action
    public static final Color PRIMARY_HOVER = new Color(29, 78, 216);    // #1D4ED8
    public static final Color SUCCESS_GREEN = new Color(22, 163, 74);    // #16A34A - Positive / Online
    public static final Color SUCCESS_HOVER = new Color(21, 128, 61);    // #15803D
    public static final Color DANGER_RED = new Color(220, 38, 38);       // #DC2626 - Delete / Offline
    public static final Color DANGER_HOVER = new Color(185, 28, 28);      // #B91C1C
    public static final Color WARNING_ORANGE = new Color(217, 119, 6);   // #D97706 - Warning / Latency

    // Neutral Surfaces & Texts
    public static final Color BG_DARK_HEADER = new Color(15, 23, 42);    // #0F172A - Header bar
    public static final Color BG_CANVAS = new Color(241, 245, 249);      // #F1F5F9 - Page canvas
    public static final Color CARD_BG = new Color(255, 255, 255);         // White
    public static final Color BORDER_LIGHT = new Color(226, 232, 240);    // #E2E8F0
    public static final Color BORDER_DARK = new Color(203, 213, 225);     // #CBD5E1

    public static final Color TEXT_PRIMARY = new Color(15, 23, 42);      // #0F172A - High contrast
    public static final Color TEXT_MUTED = new Color(100, 116, 139);     // #64748B - Subtitles
    public static final Color TEXT_LIGHT = new Color(255, 255, 255);     // White text

    // Secondary / Neutral Button
    public static final Color BTN_NEUTRAL = new Color(71, 85, 105);       // #475569
    public static final Color BTN_NEUTRAL_HOVER = new Color(51, 65, 85); // #334155

    // Standard Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_SUBHEADER = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    /**
     * Style a button with BasicButtonUI override so background and text colors render
     * 100% reliably across Windows 10/11 System Look-and-Feel and all Java runtimes.
     */
    public static void styleButton(JButton btn, Color bgColor, Color hoverColor, Color textColor) {
        // Force BasicButtonUI so Windows LookAndFeel doesn't override the background
        btn.setUI(new BasicButtonUI());

        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(bgColor);
        btn.setForeground(textColor);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 1, true),
            BorderFactory.createEmptyBorder(8, 18, 8, 18)
        ));

        // Smooth hover effect
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(hoverColor);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(bgColor);
                }
            }
        });
    }

    public static void stylePrimaryButton(JButton btn) {
        styleButton(btn, PRIMARY_BLUE, PRIMARY_HOVER, TEXT_LIGHT);
    }

    public static void styleSuccessButton(JButton btn) {
        styleButton(btn, SUCCESS_GREEN, SUCCESS_HOVER, TEXT_LIGHT);
    }

    public static void styleDangerButton(JButton btn) {
        styleButton(btn, DANGER_RED, DANGER_HOVER, TEXT_LIGHT);
    }

    public static void styleNeutralButton(JButton btn) {
        styleButton(btn, BTN_NEUTRAL, BTN_NEUTRAL_HOVER, TEXT_LIGHT);
    }

    /**
     * Style a text field or password field with clean border and padding
     */
    public static void styleTextField(JTextField field) {
        field.setFont(FONT_BODY);
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(CARD_BG);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_DARK, 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }

    /**
     * Style a card panel with clean border and background
     */
    public static void styleCard(JPanel panel) {
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT, 1, true),
            BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));
    }

    /**
     * Style a JTable with professional header and grid
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(28);
        table.setGridColor(BORDER_LIGHT);
        table.setSelectionBackground(new Color(224, 231, 255)); // Soft indigo highlight
        table.setSelectionForeground(TEXT_PRIMARY);

        table.getTableHeader().setFont(FONT_SUBHEADER);
        table.getTableHeader().setBackground(PRIMARY_BLUE);
        table.getTableHeader().setForeground(TEXT_LIGHT);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(table.getTableHeader().getPreferredSize().width, 32));
    }
}
