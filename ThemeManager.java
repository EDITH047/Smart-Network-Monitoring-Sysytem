package com.networkmonitor.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    public interface ThemeListener {
        void onThemeChanged();
    }

    private static boolean isDarkMode = false;
    private static final List<ThemeListener> listeners = new ArrayList<>();

    // --- Light Mode Colors ---
    private static final Color LIGHT_BG = new Color(245, 248, 250);
    private static final Color LIGHT_CARD_BG = new Color(255, 255, 255);
    private static final Color LIGHT_TEXT = new Color(30, 41, 59);
    private static final Color LIGHT_TEXT_MUTED = new Color(100, 116, 139);
    private static final Color LIGHT_BORDER = new Color(226, 232, 240);
    private static final Color LIGHT_PRIMARY = new Color(37, 99, 235);
    private static final Color LIGHT_SUCCESS = new Color(34, 197, 94);
    private static final Color LIGHT_ERROR = new Color(220, 38, 38);
    private static final Color LIGHT_WARNING = new Color(234, 179, 8);

    // --- Dark Mode Colors ---
    private static final Color DARK_BG = new Color(15, 23, 42);
    private static final Color DARK_CARD_BG = new Color(30, 41, 59);
    private static final Color DARK_TEXT = new Color(248, 250, 252);
    private static final Color DARK_TEXT_MUTED = new Color(148, 163, 184);
    private static final Color DARK_BORDER = new Color(51, 65, 85);
    private static final Color DARK_PRIMARY = new Color(59, 130, 246);
    private static final Color DARK_SUCCESS = new Color(34, 197, 94);
    private static final Color DARK_ERROR = new Color(239, 68, 68);
    private static final Color DARK_WARNING = new Color(234, 179, 8);

    public static void toggleTheme() {
        isDarkMode = !isDarkMode;
        for (ThemeListener listener : listeners) {
            listener.onThemeChanged();
        }
    }

    public static void addThemeListener(ThemeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public static void removeThemeListener(ThemeListener listener) {
        listeners.remove(listener);
    }

    public static boolean isDarkMode() {
        return isDarkMode;
    }

    public static Color getBackgroundColor() {
        return isDarkMode ? DARK_BG : LIGHT_BG;
    }

    public static Color getCardColor() {
        return isDarkMode ? DARK_CARD_BG : LIGHT_CARD_BG;
    }

    public static Color getTextColor() {
        return isDarkMode ? DARK_TEXT : LIGHT_TEXT;
    }

    public static Color getTextMutedColor() {
        return isDarkMode ? DARK_TEXT_MUTED : LIGHT_TEXT_MUTED;
    }

    public static Color getBorderColor() {
        return isDarkMode ? DARK_BORDER : LIGHT_BORDER;
    }

    public static Color getPrimaryColor() {
        return isDarkMode ? DARK_PRIMARY : LIGHT_PRIMARY;
    }

    public static Color getSuccessColor() {
        return isDarkMode ? DARK_SUCCESS : LIGHT_SUCCESS;
    }

    public static Color getErrorColor() {
        return isDarkMode ? DARK_ERROR : LIGHT_ERROR;
    }

    public static Color getWarningColor() {
        return isDarkMode ? DARK_WARNING : LIGHT_WARNING;
    }
    
    // Status specific background colors for badges/panels
    public static Color getSuccessBgColor() {
        return isDarkMode ? new Color(6, 78, 59) : new Color(220, 252, 231);
    }
    
    public static Color getErrorBgColor() {
        return isDarkMode ? new Color(127, 29, 29) : new Color(254, 226, 226);
    }
    
    public static Color getWarningBgColor() {
        return isDarkMode ? new Color(113, 63, 18) : new Color(254, 243, 199);
    }
}
