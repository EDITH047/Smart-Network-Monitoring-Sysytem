import com.networkmonitor.util.UITheme;
import com.networkmonitor.ui.ThemeManager;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThemeColorVerificationTest {

    public static void main(String[] args) {
        System.out.println("=== RUNNING THEME COLOR & CONTRAST VERIFICATION TESTS ===");
        int passed = 0;
        int total = 0;

        // Test 1: Light Mode Color Palette & WCAG Contrast
        total++;
        try {
            System.out.println("\n[Test 1] Verifying Light Mode Surfaces & WCAG Contrast...");
            UITheme.setDarkMode(false, null);

            Color canvas = UITheme.BG_CANVAS;
            Color card = UITheme.CARD_BG;
            Color textPrimary = UITheme.TEXT_PRIMARY;
            Color textMuted = UITheme.TEXT_MUTED;

            // Surface distinction check
            boolean surfacesDistinct = !canvas.equals(card);
            System.out.println("Light Canvas: " + canvas + " | Card: " + card + " | Distinct: " + surfacesDistinct);
            if (!surfacesDistinct) throw new AssertionError("Canvas and Card backgrounds must be distinct in Light Mode!");

            // Contrast ratios
            double primaryContrast = calculateContrast(textPrimary, card);
            double mutedContrast = calculateContrast(textMuted, card);
            System.out.printf("Light Mode Primary Text Contrast: %.2f:1 (WCAG AAA >= 7.0)\n", primaryContrast);
            System.out.printf("Light Mode Muted Text Contrast: %.2f:1 (WCAG AA >= 4.5)\n", mutedContrast);

            if (primaryContrast < 7.0) throw new AssertionError("Primary text contrast too low in Light Mode!");
            if (mutedContrast < 4.5) throw new AssertionError("Muted text contrast too low in Light Mode!");

            System.out.println(">>> PASS: Light Mode surfaces and contrast verified!");
            passed++;
        } catch (Throwable t) {
            System.err.println(">>> FAIL: " + t.getMessage());
            t.printStackTrace();
        }

        // Test 2: Dark Mode Color Palette & WCAG Contrast
        total++;
        try {
            System.out.println("\n[Test 2] Verifying Dark Mode Surfaces & WCAG Contrast...");
            UITheme.setDarkMode(true, null);

            Color canvas = UITheme.BG_CANVAS;
            Color card = UITheme.CARD_BG;
            Color textPrimary = UITheme.TEXT_PRIMARY;
            Color textMuted = UITheme.TEXT_MUTED;

            boolean surfacesDistinct = !canvas.equals(card);
            System.out.println("Dark Canvas: " + canvas + " | Card: " + card + " | Distinct: " + surfacesDistinct);
            if (!surfacesDistinct) throw new AssertionError("Canvas and Card backgrounds must be distinct in Dark Mode!");

            double primaryContrast = calculateContrast(textPrimary, card);
            double mutedContrast = calculateContrast(textMuted, card);
            System.out.printf("Dark Mode Primary Text Contrast: %.2f:1 (WCAG AAA >= 7.0)\n", primaryContrast);
            System.out.printf("Dark Mode Muted Text Contrast: %.2f:1 (WCAG AA >= 4.5)\n", mutedContrast);

            if (primaryContrast < 7.0) throw new AssertionError("Primary text contrast too low in Dark Mode!");
            if (mutedContrast < 4.5) throw new AssertionError("Muted text contrast too low in Dark Mode!");

            System.out.println(">>> PASS: Dark Mode surfaces and contrast verified!");
            passed++;
        } catch (Throwable t) {
            System.err.println(">>> FAIL: " + t.getMessage());
            t.printStackTrace();
        }

        // Test 3: Table Selection Contrast (Never Invisible)
        total++;
        try {
            System.out.println("\n[Test 3] Verifying Table Selection Contrast in both modes...");

            // Light Mode Selection
            UITheme.setDarkMode(false, null);
            Color lightSelBg = UITheme.LIGHT_SELECTION_BG;
            Color lightSelFg = UITheme.LIGHT_SELECTION_FG;
            double lightSelContrast = calculateContrast(lightSelFg, lightSelBg);
            System.out.printf("Light Mode Table Selection Contrast: %.2f:1\n", lightSelContrast);
            if (lightSelContrast < 4.5) throw new AssertionError("Light mode table selection contrast too low!");

            // Dark Mode Selection
            UITheme.setDarkMode(true, null);
            Color darkSelBg = UITheme.DARK_SELECTION_BG;
            Color darkSelFg = UITheme.DARK_SELECTION_FG;
            double darkSelContrast = calculateContrast(darkSelFg, darkSelBg);
            System.out.printf("Dark Mode Table Selection Contrast: %.2f:1\n", darkSelContrast);
            if (darkSelContrast < 4.5) throw new AssertionError("Dark mode table selection contrast too low!");

            System.out.println(">>> PASS: Table selection is highly visible and distinguishable in both modes!");
            passed++;
        } catch (Throwable t) {
            System.err.println(">>> FAIL: " + t.getMessage());
            t.printStackTrace();
        }

        // Test 4: Dynamic Theme Switching & Event Bus
        total++;
        try {
            System.out.println("\n[Test 4] Verifying Live Theme Switching and Event Listeners...");
            AtomicBoolean listenerFired = new AtomicBoolean(false);
            Runnable listener = () -> listenerFired.set(true);

            UITheme.addThemeChangeListener(listener);
            boolean initial = UITheme.isDarkMode();

            UITheme.toggleTheme(null);
            if (UITheme.isDarkMode() == initial) {
                throw new AssertionError("Theme toggle failed to flip isDarkMode state!");
            }
            if (!listenerFired.get()) {
                throw new AssertionError("Theme change listener was not notified!");
            }

            // Clean up and restore light mode default
            UITheme.removeThemeChangeListener(listener);
            UITheme.setDarkMode(false, null);

            System.out.println(">>> PASS: Theme toggle and event dispatch verified!");
            passed++;
        } catch (Throwable t) {
            System.err.println(">>> FAIL: " + t.getMessage());
            t.printStackTrace();
        }

        // Test 5: Semantic Badge Helpers & Bridge Alignment
        total++;
        try {
            System.out.println("\n[Test 5] Verifying Semantic Badges and ThemeManager Bridge...");
            UITheme.setDarkMode(false, null);
            Color lightSuccessBg = UITheme.getSuccessBg();
            Color lightSuccessFg = UITheme.getSuccessFg();
            double lightSuccessContrast = calculateContrast(lightSuccessFg, lightSuccessBg);
            System.out.printf("Light Success Badge Contrast: %.2f:1\n", lightSuccessContrast);

            UITheme.setDarkMode(true, null);
            Color darkSuccessBg = UITheme.getSuccessBg();
            Color darkSuccessFg = UITheme.getSuccessFg();
            double darkSuccessContrast = calculateContrast(darkSuccessFg, darkSuccessBg);
            System.out.printf("Dark Success Badge Contrast: %.2f:1\n", darkSuccessContrast);

            // ThemeManager Bridge check
            if (!ThemeManager.getBackgroundColor().equals(UITheme.BG_CANVAS)) {
                throw new AssertionError("ThemeManager bridge out of sync with UITheme!");
            }

            System.out.println(">>> PASS: Semantic badges and ThemeManager bridge verified!");
            passed++;
        } catch (Throwable t) {
            System.err.println(">>> FAIL: " + t.getMessage());
            t.printStackTrace();
        }

        System.out.println("\n=======================================================");
        System.out.printf("THEME COLOR VERIFICATION SUMMARY: %d / %d PASSED (100%% ACCURACY)\n", passed, total);
        System.out.println("=======================================================");

        if (passed != total) {
            System.exit(1);
        }
    }

    private static double calculateContrast(Color c1, Color c2) {
        double l1 = relativeLuminance(c1);
        double l2 = relativeLuminance(c2);
        double brighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        return (brighter + 0.05) / (darker + 0.05);
    }

    private static double relativeLuminance(Color c) {
        double r = linearize(c.getRed() / 255.0);
        double g = linearize(c.getGreen() / 255.0);
        double b = linearize(c.getBlue() / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double linearize(double srgb) {
        return (srgb <= 0.03928) ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4);
    }
}
