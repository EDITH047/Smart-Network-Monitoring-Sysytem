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
    public static final Color PRIMARY_BLUE = new DynamicColor("Component.accentColor", 37, 99, 235);
    public static final Color PRIMARY_HOVER = new DynamicColor("Button.default.hoverBackground", 29, 78, 216);
    public static final Color SUCCESS_GREEN = new Color(22, 163, 74);
    public static final Color SUCCESS_HOVER = new Color(21, 128, 61);
    public static final Color DANGER_RED = new Color(220, 38, 38);
    public static final Color DANGER_HOVER = new Color(185, 28, 28);
    public static final Color WARNING_ORANGE = new Color(217, 119, 6);

    // Neutral Surfaces & Texts
    public static final Color BG_DARK_HEADER = new DynamicColor("TitlePane.background", 15, 23, 42);
    public static final Color BG_CANVAS = new DynamicColor("Panel.background", 241, 245, 249);
    public static final Color CARD_BG = new DynamicColor("Panel.background", 255, 255, 255);
    public static final Color BORDER_LIGHT = new DynamicColor("Component.borderColor", 226, 232, 240);
    public static final Color BORDER_DARK = new DynamicColor("Component.focusedBorderColor", 203, 213, 225);

    public static final Color TEXT_PRIMARY = new DynamicColor("Label.foreground", 15, 23, 42);
    public static final Color TEXT_MUTED = new DynamicColor("Label.disabledForeground", 71, 85, 105);
    public static final Color TEXT_LIGHT = new Color(255, 255, 255);
    public static final Color CARD_TITLE = new DynamicColor("Label.foreground", 30, 41, 59);

    public static class DynamicColor extends Color {
        private final String key;
        private final Color fallback;
        
        public DynamicColor(String key, int r, int g, int b) {
            super(r, g, b);
            this.fallback = new Color(r, g, b);
            this.key = key;
        }
        
        private Color getCurrent() {
            Color c = UIManager.getColor(key);
            return c != null ? c : fallback;
        }
        
        @Override public int getRed() { return getCurrent().getRed(); }
        @Override public int getGreen() { return getCurrent().getGreen(); }
        @Override public int getBlue() { return getCurrent().getBlue(); }
        @Override public int getAlpha() { return getCurrent().getAlpha(); }
        @Override public int getRGB() { return getCurrent().getRGB(); }
        
        @Override
        public java.awt.PaintContext createContext(java.awt.image.ColorModel cm, java.awt.Rectangle r, java.awt.geom.Rectangle2D r2d, java.awt.geom.AffineTransform xform, java.awt.RenderingHints hints) {
            return getCurrent().createContext(cm, r, r2d, xform, hints);
        }
    }

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
     * Style a JTable with professional header and grid.
     *
     * Windows System Look-and-Feel silently ignores JTableHeader.setBackground()
     * and setForeground(), so we install a custom DefaultTableCellRenderer on
     * every header column to guarantee the colors render correctly.
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(28);
        table.setGridColor(BORDER_LIGHT);
        table.setSelectionBackground(new Color(224, 231, 255));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setForeground(TEXT_PRIMARY);

        // Custom header renderer — the ONLY reliable way to color headers on Windows L&F
        javax.swing.table.DefaultTableCellRenderer headerRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                label.setBackground(new Color(30, 41, 59));   // Dark navy
                label.setForeground(Color.WHITE);              // White text
                label.setFont(FONT_SUBHEADER);
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 1, new Color(51, 65, 85)),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
                ));
                label.setOpaque(true);
                return label;
            }
        };

        table.getTableHeader().setDefaultRenderer(headerRenderer);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(
            new Dimension(table.getTableHeader().getPreferredSize().width, 36));
    }

    /**
     * Style a JTabbedPane with high-contrast tab labels
     */
    public static void styleTabbedPane(JTabbedPane tabbedPane) {
        tabbedPane.setFont(FONT_SUBHEADER);
        tabbedPane.setForeground(TEXT_PRIMARY);     // Tab text color — dark for readability
        tabbedPane.setBackground(CARD_BG);
    }
}
