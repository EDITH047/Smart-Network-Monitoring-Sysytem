package com.networkmonitor.util;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

/**
 * UITheme - Comprehensive Centralized Theme Engine & UI Styling Framework.
 * Provides high-contrast, WCAG-compliant Light and Dark Mode palettes with
 * true surface elevation, distinct card borders, adaptive JFreeChart coloring,
 * seamless live theme switching, and persistent user preferences.
 */
public class UITheme {

    // Preferences node for persisting theme selection
    private static final Preferences PREFS = Preferences.userNodeForPackage(UITheme.class);
    private static final String PREF_DARK_MODE = "smart_monitor_dark_mode";

    // Active Theme State
    private static boolean isDark = PREFS.getBoolean(PREF_DARK_MODE, false);
    private static final List<Runnable> themeListeners = new CopyOnWriteArrayList<>();

    // ==========================================
    // ☀️ LIGHT MODE PALETTE (Clean, Modern Slate)
    // ==========================================
    public static final Color LIGHT_BG_CANVAS       = new Color(241, 245, 249); // Slate 100
    public static final Color LIGHT_CARD_BG         = new Color(255, 255, 255); // Pure White
    public static final Color LIGHT_CARD_INSET       = new Color(248, 250, 252); // Slate 50
    public static final Color LIGHT_BORDER_LIGHT     = new Color(226, 232, 240); // Slate 200
    public static final Color LIGHT_BORDER_DARK      = new Color(203, 213, 225); // Slate 300
    public static final Color LIGHT_TEXT_PRIMARY     = new Color(15, 23, 42);    // Slate 900 (16:1)
    public static final Color LIGHT_TEXT_MUTED       = new Color(71, 85, 105);   // Slate 600 (7:1)
    public static final Color LIGHT_TABLE_GRID       = new Color(226, 232, 240); // Slate 200
    public static final Color LIGHT_SELECTION_BG     = new Color(224, 231, 255); // Indigo 100
    public static final Color LIGHT_SELECTION_FG     = new Color(30, 27, 75);    // Indigo 950
    public static final Color LIGHT_HEADER_BG        = new Color(15, 23, 42);    // Slate 900
    public static final Color LIGHT_HEADER_FG        = new Color(255, 255, 255);
    public static final Color LIGHT_BTN_NEUTRAL      = new Color(71, 85, 105);   // Slate 600
    public static final Color LIGHT_BTN_NEUTRAL_HOV  = new Color(51, 65, 85);    // Slate 700

    // ==========================================
    // 🌙 DARK MODE PALETTE (Deep Obsidian Slate)
    // ==========================================
    public static final Color DARK_BG_CANVAS        = new Color(9, 13, 22);     // Obsidian Slate 950
    public static final Color DARK_CARD_BG          = new Color(22, 31, 48);    // Elevated Slate 850
    public static final Color DARK_CARD_INSET        = new Color(15, 23, 42);    // Sunken Slate 900
    public static final Color DARK_BORDER_LIGHT      = new Color(38, 53, 74);    // Slate 750
    public static final Color DARK_BORDER_DARK       = new Color(62, 79, 102);   // Slate 650
    public static final Color DARK_TEXT_PRIMARY      = new Color(248, 250, 252); // Slate 50 (15:1)
    public static final Color DARK_TEXT_MUTED        = new Color(148, 163, 184); // Slate 400 (6.5:1)
    public static final Color DARK_TABLE_GRID        = new Color(31, 43, 62);    // Slate 800
    public static final Color DARK_SELECTION_BG      = new Color(37, 99, 235);   // Electric Blue 600
    public static final Color DARK_SELECTION_FG      = new Color(255, 255, 255); // Pure White
    public static final Color DARK_HEADER_BG         = new Color(15, 23, 42);    // Slate 900
    public static final Color DARK_HEADER_FG         = new Color(248, 250, 252);
    public static final Color DARK_BTN_NEUTRAL       = new Color(47, 63, 86);    // Slate 700
    public static final Color DARK_BTN_NEUTRAL_HOV   = new Color(64, 84, 112);   // Slate 600

    // ==========================================
    // 🎨 ACCENT & SEMANTIC STATUS COLORS
    // ==========================================
    public static final Color PRIMARY_BLUE   = new DynamicThemeColor(() -> isDark ? new Color(96, 165, 250) : new Color(37, 99, 235));  // Blue 400 / 600
    public static final Color PRIMARY_HOVER  = new DynamicThemeColor(() -> isDark ? new Color(147, 197, 253) : new Color(29, 78, 216)); // Blue 300 / 700
    public static final Color SUCCESS_GREEN  = new DynamicThemeColor(() -> isDark ? new Color(52, 211, 153) : new Color(16, 185, 129)); // Emerald
    public static final Color SUCCESS_HOVER  = new DynamicThemeColor(() -> isDark ? new Color(110, 231, 183) : new Color(5, 150, 105));
    public static final Color DANGER_RED     = new DynamicThemeColor(() -> isDark ? new Color(251, 113, 133) : new Color(244, 63, 94)); // Rose
    public static final Color DANGER_HOVER   = new DynamicThemeColor(() -> isDark ? new Color(253, 164, 175) : new Color(225, 29, 72));
    public static final Color WARNING_ORANGE = new DynamicThemeColor(() -> isDark ? new Color(251, 191, 36) : new Color(245, 158, 11)); // Amber
    public static final Color TEXT_LIGHT     = new Color(255, 255, 255);

    // ==========================================
    // 🔄 DYNAMIC SURFACES & TEXT TOKENS
    // ==========================================
    public static final Color BG_DARK_HEADER = new DynamicThemeColor(() -> isDark ? DARK_HEADER_BG : LIGHT_HEADER_BG);
    public static final Color BG_CANVAS      = new DynamicThemeColor(() -> isDark ? DARK_BG_CANVAS : LIGHT_BG_CANVAS);
    public static final Color CARD_BG        = new DynamicThemeColor(() -> isDark ? DARK_CARD_BG : LIGHT_CARD_BG);
    public static final Color CARD_INSET     = new DynamicThemeColor(() -> isDark ? DARK_CARD_INSET : LIGHT_CARD_INSET);
    public static final Color BORDER_LIGHT   = new DynamicThemeColor(() -> isDark ? DARK_BORDER_LIGHT : LIGHT_BORDER_LIGHT);
    public static final Color BORDER_DARK    = new DynamicThemeColor(() -> isDark ? DARK_BORDER_DARK : LIGHT_BORDER_DARK);
    public static final Color TEXT_PRIMARY   = new DynamicThemeColor(() -> isDark ? DARK_TEXT_PRIMARY : LIGHT_TEXT_PRIMARY);
    public static final Color TEXT_MUTED     = new DynamicThemeColor(() -> isDark ? DARK_TEXT_MUTED : LIGHT_TEXT_MUTED);
    public static final Color CARD_TITLE     = new DynamicThemeColor(() -> isDark ? DARK_TEXT_PRIMARY : LIGHT_TEXT_PRIMARY);

    public static final Color BTN_NEUTRAL       = new DynamicThemeColor(() -> isDark ? DARK_BTN_NEUTRAL : LIGHT_BTN_NEUTRAL);
    public static final Color BTN_NEUTRAL_HOVER = new DynamicThemeColor(() -> isDark ? DARK_BTN_NEUTRAL_HOV : LIGHT_BTN_NEUTRAL_HOV);

    // ==========================================
    // 📐 TYPOGRAPHY
    // ==========================================
    public static final Font FONT_TITLE      = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADER     = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_SUBHEADER  = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY_BOLD  = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL      = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 11);
    public static final Font FONT_STAT       = new Font("Segoe UI", Font.BOLD, 20);

    /**
     * Smart dynamic Color subclass that delegates color retrieval to a runtime supplier,
     * allowing static Color constants to instantly reflect the active theme.
     */
    public static class DynamicThemeColor extends Color {
        private final Supplier<Color> supplier;

        public DynamicThemeColor(Supplier<Color> supplier) {
            super(0, 0, 0);
            this.supplier = supplier;
        }

        private Color current() {
            Color c = supplier.get();
            return c != null ? c : Color.BLACK;
        }

        @Override public int getRed() { return current().getRed(); }
        @Override public int getGreen() { return current().getGreen(); }
        @Override public int getBlue() { return current().getBlue(); }
        @Override public int getAlpha() { return current().getAlpha(); }
        @Override public int getRGB() { return current().getRGB(); }
        @Override public Color brighter() { return current().brighter(); }
        @Override public Color darker() { return current().darker(); }
        @Override public float[] getRGBComponents(float[] comp) { return current().getRGBComponents(comp); }
        @Override public float[] getRGBColorComponents(float[] comp) { return current().getRGBColorComponents(comp); }
        @Override public PaintContext createContext(java.awt.image.ColorModel cm, Rectangle r,
                                                    java.awt.geom.Rectangle2D r2d, java.awt.geom.AffineTransform xform,
                                                    RenderingHints hints) {
            return current().createContext(cm, r, r2d, xform, hints);
        }
    }

    // ==========================================
    // 🌓 THEME STATE MANAGEMENT & EVENT BUS
    // ==========================================

    public static boolean isDarkMode() {
        return isDark;
    }

    /**
     * Initializes FlatLaf with the user's stored theme preference.
     * Must be called at application startup before opening any windows.
     */
    public static void initTheme() {
        isDark = PREFS.getBoolean(PREF_DARK_MODE, false);
        try {
            if (isDark) {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
            }
            applyGlobalFlatLafSettings();
        } catch (Exception ex) {
            System.err.println("[UITheme] Could not initialize FlatLaf theme: " + ex.getMessage());
        }
    }

    /**
     * Toggles between Dark Mode and Light Mode, persists the preference,
     * updates all open Swing component trees, and fires theme change events.
     */
    public static void toggleTheme(Window rootWindow) {
        setDarkMode(!isDark, rootWindow);
    }

    public static void setDarkMode(boolean dark, Window rootWindow) {
        isDark = dark;
        PREFS.putBoolean(PREF_DARK_MODE, isDark);

        try {
            if (isDark) {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
            }
            applyGlobalFlatLafSettings();

            for (Window window : Window.getWindows()) {
                if (window.isDisplayable()) {
                    SwingUtilities.updateComponentTreeUI(window);
                }
            }
        } catch (Exception ex) {
            System.err.println("[UITheme] Error switching LookAndFeel: " + ex.getMessage());
        }

        notifyThemeChange();
    }

    public static void addThemeChangeListener(Runnable listener) {
        if (!themeListeners.contains(listener)) {
            themeListeners.add(listener);
        }
    }

    public static void removeThemeChangeListener(Runnable listener) {
        themeListeners.remove(listener);
    }

    public static void notifyThemeChange() {
        for (Runnable listener : themeListeners) {
            try {
                listener.run();
            } catch (Exception ex) {
                System.err.println("[UITheme] Listener error on theme change: " + ex.getMessage());
            }
        }
    }

    // ==========================================
    // 🏷️ SEMANTIC BADGE STYLING HELPERS
    // ==========================================

    public static Color getSuccessBg() {
        return isDark ? new Color(6, 78, 59) : new Color(220, 252, 231); // Green 900 / 100
    }
    public static Color getSuccessFg() {
        return isDark ? new Color(74, 222, 128) : new Color(21, 128, 61); // Green 400 / 700
    }
    public static Color getSuccessBorder() {
        return isDark ? new Color(5, 150, 105) : new Color(134, 239, 172); // Green 600 / 300
    }

    public static Color getWarningBg() {
        return isDark ? new Color(120, 53, 15) : new Color(254, 243, 199); // Amber 900 / 100
    }
    public static Color getWarningFg() {
        return isDark ? new Color(251, 191, 36) : new Color(180, 83, 9);   // Amber 400 / 800
    }
    public static Color getWarningBorder() {
        return isDark ? new Color(217, 119, 6) : new Color(252, 211, 77);  // Amber 600 / 300
    }

    public static Color getDangerBg() {
        return isDark ? new Color(127, 29, 29) : new Color(254, 226, 226); // Rose 900 / 100
    }
    public static Color getDangerFg() {
        return isDark ? new Color(248, 113, 113) : new Color(185, 28, 28); // Rose 400 / 700
    }
    public static Color getDangerBorder() {
        return isDark ? new Color(220, 38, 38) : new Color(252, 165, 165); // Rose 600 / 300
    }

    public static Color getInfoBg() {
        return isDark ? new Color(30, 27, 75) : new Color(224, 231, 255);  // Indigo 950 / 100
    }
    public static Color getInfoFg() {
        return isDark ? new Color(129, 140, 248) : new Color(55, 48, 163); // Indigo 400 / 800
    }
    public static Color getInfoBorder() {
        return isDark ? new Color(79, 70, 229) : new Color(165, 180, 252); // Indigo 600 / 300
    }

    // ==========================================
    // 🛠️ COMPONENT STYLING UTILITIES
    // ==========================================

    public static void applyGlobalFlatLafSettings() {
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("ProgressBar.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("TabbedPane.tabInsets", new Insets(8, 20, 8, 20));
        UIManager.put("TabbedPane.showTabSeparators", true);
    }

    public static void styleButton(JButton btn, Color bgColor, Color hoverColor, Color textColor) {
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

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(hoverColor);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(bgColor);
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

    public static void styleCard(JPanel panel) {
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT, 1, true),
            BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));
    }

    /**
     * Styles a JTable with high-contrast, theme-aware headers, alternating rows,
     * crisp gridlines, and high-visibility row selections that are never invisible.
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(34);
        table.setBackground(CARD_BG);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(new DynamicThemeColor(() -> isDark ? DARK_TABLE_GRID : LIGHT_TABLE_GRID));
        table.setSelectionBackground(new DynamicThemeColor(() -> isDark ? DARK_SELECTION_BG : LIGHT_SELECTION_BG));
        table.setSelectionForeground(new DynamicThemeColor(() -> isDark ? DARK_SELECTION_FG : LIGHT_SELECTION_FG));
        table.setShowGrid(true);

        // Custom theme-aware header renderer
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                label.setBackground(isDark ? DARK_HEADER_BG : LIGHT_HEADER_BG);
                label.setForeground(isDark ? DARK_HEADER_FG : LIGHT_HEADER_FG);
                label.setFont(FONT_SUBHEADER);
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 1, isDark ? new Color(47, 63, 86) : new Color(51, 65, 85)),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
                label.setOpaque(true);
                return label;
            }
        };

        table.getTableHeader().setDefaultRenderer(headerRenderer);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(
            new Dimension(table.getTableHeader().getPreferredSize().width, 38));
    }

    public static void styleTabbedPane(JTabbedPane tabbedPane) {
        tabbedPane.setFont(FONT_SUBHEADER);
        tabbedPane.setForeground(TEXT_PRIMARY);
        tabbedPane.setBackground(CARD_BG);
    }

    public static JPanel createSearchBar(JTable table) {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        searchPanel.setOpaque(false);

        JLabel searchIcon = new JLabel("🔍 Search:");
        searchIcon.setFont(FONT_BODY_BOLD);
        searchIcon.setForeground(TEXT_PRIMARY);

        JTextField searchField = new JTextField(15);
        styleTextField(searchField);
        searchField.putClientProperty("JTextField.placeholderText", "Filter...");

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void updateFilter() {
                javax.swing.table.TableRowSorter<?> sorter = (javax.swing.table.TableRowSorter<?>) table.getRowSorter();
                if (sorter != null) {
                    String text = searchField.getText();
                    if (text.trim().isEmpty()) {
                        sorter.setRowFilter(null);
                    } else {
                        sorter.setRowFilter(javax.swing.RowFilter.regexFilter("(?i)" + text));
                    }
                }
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updateFilter(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updateFilter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateFilter(); }
        });

        searchPanel.add(searchIcon);
        searchPanel.add(searchField);
        return searchPanel;
    }
}
