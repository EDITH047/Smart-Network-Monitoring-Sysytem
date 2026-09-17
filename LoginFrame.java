package com.networkmonitor.ui;

import com.networkmonitor.model.User;
import com.networkmonitor.service.AuthService;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * LoginFrame - Professional User Authentication UI
 */
public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton exitButton;
    private JLabel errorLabel;
    private AuthService authService;

    public LoginFrame() {
        this.authService = AuthService.getInstance();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Smart Network Monitor — Authentication");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 480);
        setLocationRelativeTo(null);
        setResizable(false);

        // Canvas Panel
        JPanel rootPanel = new JPanel(new GridBagLayout());
        rootPanel.setBackground(UITheme.BG_CANVAS);

        // Card Container
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setPreferredSize(new Dimension(380, 400));
        UITheme.styleCard(cardPanel);

        // 1. Icon & Header
        JLabel logoLabel = new JLabel("🌐", SwingConstants.CENTER);
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(logoLabel);

        cardPanel.add(Box.createVerticalStrut(6));

        JLabel titleLabel = new JLabel("Network Monitor System");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.PRIMARY_BLUE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(titleLabel);

        JLabel subtitleLabel = new JLabel("Sign in to access your dashboard");
        subtitleLabel.setFont(UITheme.FONT_SMALL);
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(subtitleLabel);

        cardPanel.add(Box.createVerticalStrut(20));

        // 2. Username Input
        JLabel uLabel = new JLabel("Username");
        uLabel.setFont(UITheme.FONT_BODY_BOLD);
        uLabel.setForeground(UITheme.TEXT_PRIMARY);
        uLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(uLabel);

        cardPanel.add(Box.createVerticalStrut(4));

        usernameField = new JTextField();
        UITheme.styleTextField(usernameField);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cardPanel.add(usernameField);

        cardPanel.add(Box.createVerticalStrut(14));

        // 3. Password Input
        JLabel pLabel = new JLabel("Password");
        pLabel.setFont(UITheme.FONT_BODY_BOLD);
        pLabel.setForeground(UITheme.TEXT_PRIMARY);
        pLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(pLabel);

        cardPanel.add(Box.createVerticalStrut(4));

        passwordField = new JPasswordField();
        UITheme.styleTextField(passwordField);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        });
        cardPanel.add(passwordField);

        cardPanel.add(Box.createVerticalStrut(10));

        // Error Feedback Label
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.DANGER_RED);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(errorLabel);

        cardPanel.add(Box.createVerticalStrut(14));

        // 4. Buttons Container
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        loginButton = new JButton("Sign In");
        UITheme.stylePrimaryButton(loginButton);
        loginButton.addActionListener(e -> handleLogin());

        exitButton = new JButton("Exit");
        UITheme.styleNeutralButton(exitButton);
        exitButton.addActionListener(e -> System.exit(0));

        btnRow.add(loginButton);
        btnRow.add(exitButton);
        cardPanel.add(btnRow);

        cardPanel.add(Box.createVerticalStrut(18));

        // Demo credentials hint
        JPanel hintBox = new JPanel(new FlowLayout(FlowLayout.CENTER));
        hintBox.setOpaque(true);
        hintBox.setBackground(new Color(238, 242, 255));
        hintBox.setBorder(BorderFactory.createLineBorder(new Color(199, 210, 254), 1, true));
        hintBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel hintLabel = new JLabel("Default Admin: admin / admin123");
        hintLabel.setFont(UITheme.FONT_SMALL);
        hintLabel.setForeground(UITheme.PRIMARY_BLUE);
        hintBox.add(hintLabel);
        cardPanel.add(hintBox);

        rootPanel.add(cardPanel);
        add(rootPanel);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        User user = authService.login(username, password, "127.0.0.1");

        if (user != null) {
            clearFields();
            openDashboard(user);
            dispose();
        } else {
            showError("Invalid username or password");
            passwordField.setText("");
            passwordField.requestFocus();
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        errorLabel.setText(" ");
    }

    private void openDashboard(User user) {
        SwingUtilities.invokeLater(() -> {
            MainDashboard dashboard = new MainDashboard(user);
            dashboard.setVisible(true);
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
