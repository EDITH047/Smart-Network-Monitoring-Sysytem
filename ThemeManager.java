package com.networkmonitor.ui;

import com.networkmonitor.util.UITheme;
import java.awt.Color;

/**
 * ThemeManager - Bridge layer delegating to UITheme engine.
 * Maintained for backwards compatibility and clean API access.
 */
public class ThemeManager {
    public interface ThemeListener {
        void onThemeChanged();
    }

    public static void toggleTheme() {
        UITheme.toggleTheme(null);
    }

    public static void addThemeListener(ThemeListener listener) {
        UITheme.addThemeChangeListener(listener::onThemeChanged);
    }

    public static boolean isDarkMode() {
        return UITheme.isDarkMode();
    }

    public static Color getBackgroundColor() {
        return UITheme.BG_CANVAS;
    }

    public static Color getCardColor() {
        return UITheme.CARD_BG;
    }

    public static Color getTextColor() {
        return UITheme.TEXT_PRIMARY;
    }

    public static Color getTextMutedColor() {
        return UITheme.TEXT_MUTED;
    }

    public static Color getBorderColor() {
        return UITheme.BORDER_LIGHT;
    }

    public static Color getPrimaryColor() {
        return UITheme.PRIMARY_BLUE;
    }

    public static Color getSuccessColor() {
        return UITheme.SUCCESS_GREEN;
    }

    public static Color getErrorColor() {
        return UITheme.DANGER_RED;
    }

    public static Color getWarningColor() {
        return UITheme.WARNING_ORANGE;
    }

    public static Color getSuccessBgColor() {
        return UITheme.getSuccessBg();
    }

    public static Color getErrorBgColor() {
        return UITheme.getDangerBg();
    }

    public static Color getWarningBgColor() {
        return UITheme.getWarningBg();
    }
}
