package com.networkmonitor.ui;

import com.networkmonitor.service.AuthService;
import com.networkmonitor.model.User;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * LoginFrame - User authentication UI
 * Displayed when application starts, handles login/logout
 */
public class LoginFrame extends JFrame implements ThemeManager.ThemeListener {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton exitButton;
    private JLabel errorLabel;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JLabel infoLabel;
    private JPanel mainPanel;
    private JPanel buttonPanel;
    private AuthService authService;

    public LoginFrame() {
        this.authService = AuthService.getInstance();
        initializeUI();
        ThemeManager.addThemeListener(this);
        applyTheme();
    }
    
    @Override
    public void onThemeChanged() {
        applyTheme();
    }
    
    private void applyTheme() {
        mainPanel.setBackground(ThemeManager.getBackgroundColor());
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());
        
        titleLabel.setForeground(ThemeManager.getPrimaryColor());
        subtitleLabel.setForeground(ThemeManager.getTextMutedColor());
        
        usernameLabel.setForeground(ThemeManager.getTextColor());
        usernameField.setBackground(ThemeManager.getCardColor());
        usernameField.setForeground(ThemeManager.getTextColor());
        usernameField.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        
        passwordLabel.setForeground(ThemeManager.getTextColor());
        passwordField.setBackground(ThemeManager.getCardColor());
        passwordField.setForeground(ThemeManager.getTextColor());
        passwordField.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        
        errorLabel.setForeground(ThemeManager.getErrorColor());
        infoLabel.setForeground(ThemeManager.getTextMutedColor());
        
        loginButton.setBackground(ThemeManager.getPrimaryColor());
        loginButton.setForeground(Color.WHITE);
        
        exitButton.setBackground(ThemeManager.getTextMutedColor());
        exitButton.setForeground(Color.WHITE);
        
        mainPanel.repaint();
    }

    /**
     * Initialize UI components
     */
    private void initializeUI() {
        setTitle("Smart Network Monitoring System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(false);

        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Error setting look and feel: " + e.getMessage());
        }

        // Main panel
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

        // Title
        titleLabel = new JLabel("Network Monitoring System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);

        // Subtitle
        subtitleLabel = new JLabel("Secure Login");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(subtitleLabel);

        mainPanel.add(Box.createVerticalStrut(30));

        // Username section
        usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        mainPanel.add(usernameLabel);

        usernameField = new JTextField();
        usernameField.setFont(new Font("Arial", Font.PLAIN, 12));
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        mainPanel.add(usernameField);

        mainPanel.add(Box.createVerticalStrut(15));

        // Password section
        passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        mainPanel.add(passwordLabel);

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Arial", Font.PLAIN, 12));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        passwordField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loginButton.doClick();
                }
            }
        });
        mainPanel.add(passwordField);

        mainPanel.add(Box.createVerticalStrut(20));

        // Error label
        errorLabel = new JLabel();
        errorLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(errorLabel);

        mainPanel.add(Box.createVerticalStrut(15));

        // Button panel
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Arial", Font.BOLD, 12));
        loginButton.setBorder(BorderFactory.createEmptyBorder(8, 30, 8, 30));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.addActionListener(this::handleLogin);

        exitButton = new JButton("Exit");
        exitButton.setFont(new Font("Arial", Font.BOLD, 12));
        exitButton.setBorder(BorderFactory.createEmptyBorder(8, 30, 8, 30));
        exitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exitButton.addActionListener(e -> System.exit(0));

        buttonPanel.add(loginButton);
        buttonPanel.add(Box.createHorizontalStrut(15));
        buttonPanel.add(exitButton);

        mainPanel.add(buttonPanel);

        // Demo credentials info
        mainPanel.add(Box.createVerticalStrut(25));
        infoLabel = new JLabel("Demo: admin / admin123");
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(infoLabel);

        add(mainPanel);
    }

    /**
     * Handle login button click
     */
    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Validate inputs
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password");
            return;
        }

        // Attempt login
        User user = authService.login(username, password, "127.0.0.1");

        if (user != null) {
            // Login successful
            System.out.println("[LoginFrame] Login successful for: " + username);
            ThemeManager.removeThemeListener(this);
            clearFields();
            openMainDashboard(user);
            dispose(); // Close login window
        } else {
            // Login failed
            showError("Invalid username or password");
            passwordField.setText("");
            passwordField.requestFocus();
        }
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        errorLabel.setText(message);
        System.out.println("[LoginFrame] Login error: " + message);
    }

    /**
     * Clear input fields
     */
    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        errorLabel.setText("");
    }

    /**
     * Open main dashboard on successful login
     */
    private void openMainDashboard(User user) {
        SwingUtilities.invokeLater(() -> {
            MainDashboard dashboard = new MainDashboard(user);
            dashboard.setVisible(true);
        });
    }

    /**
     * Main method - start application
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
