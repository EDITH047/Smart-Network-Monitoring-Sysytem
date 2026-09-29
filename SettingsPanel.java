package com.networkmonitor.ui;

import com.networkmonitor.model.User;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JPanel {
    private User currentUser;

    public SettingsPanel(User currentUser) {
        this.currentUser = currentUser;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("⚙️ Settings");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        UITheme.styleCard(centerPanel);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JLabel themeLabel = new JLabel("Theme Settings");
        themeLabel.setFont(UITheme.FONT_SUBHEADER);
        centerPanel.add(themeLabel);
        centerPanel.add(Box.createVerticalStrut(10));

        JButton toggleThemeBtn = new JButton("Toggle Dark/Light Mode");
        UITheme.stylePrimaryButton(toggleThemeBtn);
        toggleThemeBtn.addActionListener(e -> {
            try {
                if (UIManager.getLookAndFeel().getClass().getName().contains("FlatDarkLaf")) {
                    UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
                } else {
                    UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
                }
                SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(this));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        
        centerPanel.add(toggleThemeBtn);

        add(centerPanel, BorderLayout.CENTER);
    }
}
