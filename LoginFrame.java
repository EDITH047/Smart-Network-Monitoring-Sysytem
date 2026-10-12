package com.networkmonitor.ui;

import com.networkmonitor.model.User;
import com.networkmonitor.service.AuthService;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * LoginFrame — Executive Split-Screen Authentication Portal.
 * Features:
 * - Left Hero: High-tech indigo/slate gradient with branding, system status, and feature pillars
 * - Right Form: Spacious modern card with floating labels, 1-click demo chips, and smooth validation
 */
public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton exitButton;
    private JPanel errorBanner;
    private JLabel errorLabel;
    private AuthService authService;

    public LoginFrame() {
        this.authService = AuthService.getInstance();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Smart Network Monitor — Enterprise Operations Portal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(880, 560);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel rootContainer = new JPanel(new GridLayout(1, 2, 0, 0));
        rootContainer.setBackground(UITheme.BG_CANVAS);

        // 1. Left Hero Panel
        JPanel heroPanel = createHeroPanel();
        rootContainer.add(heroPanel);

        // 2. Right Form Panel
        JPanel formPanel = createFormPanel();
        rootContainer.add(formPanel);

        setContentPane(rootContainer);
    }

    private JPanel createHeroPanel() {
        JPanel hero = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Deep Dark Slate-to-Indigo Gradient
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(15, 23, 42),       // Slate 900
                    0, getHeight(), new Color(30, 27, 75) // Indigo 950
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Subtle Decorative Tech Grid Circles
                g2.setColor(new Color(99, 102, 241, 20)); // Faint Indigo
                g2.drawOval(-60, -60, 260, 260);
                g2.drawOval(getWidth() - 120, getHeight() - 140, 300, 300);
            }
        };

        hero.setLayout(new BorderLayout());
        hero.setBorder(BorderFactory.createEmptyBorder(44, 40, 36, 40));

        // Top Brand Title
        JPanel brandTop = new JPanel();
        brandTop.setLayout(new BoxLayout(brandTop, BoxLayout.Y_AXIS));
        brandTop.setOpaque(false);

        JLabel logoBadge = new JLabel("🌐", SwingConstants.CENTER);
        logoBadge.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 44));
        logoBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandTop.add(logoBadge);

        brandTop.add(Box.createVerticalStrut(12));

        JLabel titleLbl = new JLabel("SMART NETWORK MONITOR");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandTop.add(titleLbl);

        brandTop.add(Box.createVerticalStrut(4));

        JLabel subLbl = new JLabel("Enterprise Operations Center & Intelligence");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl.setForeground(new Color(148, 163, 184)); // Slate 400
        subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandTop.add(subLbl);

        hero.add(brandTop, BorderLayout.NORTH);

        // Center Feature Pillars
        JPanel centerPillars = new JPanel();
        centerPillars.setLayout(new BoxLayout(centerPillars, BoxLayout.Y_AXIS));
        centerPillars.setOpaque(false);
        centerPillars.add(Box.createVerticalStrut(28));

        centerPillars.add(createFeatureItem("⚡", "Real-Time Telemetry", "Live sub-second bandwidth & latency metrics"));
        centerPillars.add(Box.createVerticalStrut(16));
        centerPillars.add(createFeatureItem("🛡️", "Active Threat Defense", "Automated DDoS & brute force packet containment"));
        centerPillars.add(Box.createVerticalStrut(16));
        centerPillars.add(createFeatureItem("🗺️", "Interactive Topology", "Dynamic node drag-and-drop & active LAN subnet sweep"));

        hero.add(centerPillars, BorderLayout.CENTER);

        // Bottom Status Badge
        JPanel bottomStatus = new JPanel(new BorderLayout());
        bottomStatus.setOpaque(false);

        JLabel statusPill = new JLabel("  ● System Online • MySQL 8.0 • Engine Ready  ");
        statusPill.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusPill.setForeground(UITheme.SUCCESS_GREEN);
        statusPill.setOpaque(true);
        statusPill.setBackground(new Color(6, 78, 59, 180)); // Deep emerald glass
        statusPill.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(16, 185, 129, 100), 1, true),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        bottomStatus.add(statusPill, BorderLayout.WEST);

        JLabel verLbl = new JLabel("v2.4 Enterprise");
        verLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        verLbl.setForeground(new Color(100, 116, 139));
        bottomStatus.add(verLbl, BorderLayout.EAST);

        hero.add(bottomStatus, BorderLayout.SOUTH);
        return hero;
    }

    private JPanel createFeatureItem(String icon, String title, String desc) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);

        JLabel iconLbl = new JLabel(icon, SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        iconLbl.setPreferredSize(new Dimension(32, 32));
        row.add(iconLbl, BorderLayout.WEST);

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.setForeground(new Color(241, 245, 249));
        textCol.add(t);

        JLabel d = new JLabel(desc);
        d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        d.setForeground(new Color(148, 163, 184));
        textCol.add(d);

        row.add(textCol, BorderLayout.CENTER);
        return row;
    }

    private JPanel createFormPanel() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(UITheme.CARD_BG);
        form.setBorder(BorderFactory.createEmptyBorder(44, 48, 36, 48));

        // Header row with Greetings & Theme Switcher
        JPanel topHeaderRow = new JPanel(new BorderLayout());
        topHeaderRow.setOpaque(false);
        topHeaderRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topHeaderRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel welcomeLbl = new JLabel("Welcome Back");
        welcomeLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        welcomeLbl.setForeground(UITheme.TEXT_PRIMARY);
        topHeaderRow.add(welcomeLbl, BorderLayout.WEST);

        JButton themeToggleBtn = new JButton(UITheme.isDarkMode() ? "☀️ Light" : "🌙 Dark");
        themeToggleBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        themeToggleBtn.setFocusPainted(false);
        themeToggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        themeToggleBtn.addActionListener(e -> {
            UITheme.toggleTheme(this);
            themeToggleBtn.setText(UITheme.isDarkMode() ? "☀️ Light" : "🌙 Dark");
        });
        topHeaderRow.add(themeToggleBtn, BorderLayout.EAST);
        form.add(topHeaderRow);

        form.add(Box.createVerticalStrut(4));

        JLabel promptLbl = new JLabel("Enter your operator credentials to access the NOC dashboard");
        promptLbl.setFont(UITheme.FONT_BODY);
        promptLbl.setForeground(UITheme.TEXT_MUTED);
        promptLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(promptLbl);

        form.add(Box.createVerticalStrut(24));

        // Error Feedback Alert Banner
        errorBanner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        errorBanner.setOpaque(true);
        errorBanner.setBackground(UITheme.getDangerBg());
        errorBanner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.getDangerBorder(), 1, true),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        errorBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        errorBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorBanner.setVisible(false);

        errorLabel = new JLabel("⚠️ Invalid credentials.");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.DANGER_RED);
        errorBanner.add(errorLabel);
        form.add(errorBanner);

        form.add(Box.createVerticalStrut(10));

        // Username
        JLabel uLabel = new JLabel("Username or Operator ID");
        uLabel.setFont(UITheme.FONT_BODY_BOLD);
        uLabel.setForeground(UITheme.TEXT_PRIMARY);
        uLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(uLabel);

        form.add(Box.createVerticalStrut(6));

        usernameField = new JTextField("admin");
        UITheme.styleTextField(usernameField);
        usernameField.setPreferredSize(new Dimension(380, 38));
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(usernameField);

        form.add(Box.createVerticalStrut(14));

        // Password
        JLabel pLabel = new JLabel("Password");
        pLabel.setFont(UITheme.FONT_BODY_BOLD);
        pLabel.setForeground(UITheme.TEXT_PRIMARY);
        pLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(pLabel);

        form.add(Box.createVerticalStrut(6));

        passwordField = new JPasswordField("admin123");
        UITheme.styleTextField(passwordField);
        passwordField.setPreferredSize(new Dimension(380, 38));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        });
        form.add(passwordField);

        form.add(Box.createVerticalStrut(18));

        // Quick Demo Fill Chips
        JPanel demoBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        demoBox.setOpaque(false);
        demoBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel quickLbl = new JLabel("Quick Demo:");
        quickLbl.setFont(UITheme.FONT_SMALL);
        quickLbl.setForeground(UITheme.TEXT_MUTED);
        demoBox.add(quickLbl);

        JButton adminChip = createChipButton("👑 Admin", "admin", "admin123");
        JButton opChip = createChipButton("🛠️ Operator", "operator", "operator123");
        demoBox.add(adminChip);
        demoBox.add(opChip);
        form.add(demoBox);

        form.add(Box.createVerticalStrut(24));

        // Login Action Buttons
        loginButton = new JButton("Sign In to Dashboard  →");
        UITheme.stylePrimaryButton(loginButton);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        loginButton.setPreferredSize(new Dimension(380, 42));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.addActionListener(e -> handleLogin());
        form.add(loginButton);

        form.add(Box.createVerticalStrut(10));

        exitButton = new JButton("Exit Application");
        UITheme.styleNeutralButton(exitButton);
        exitButton.setPreferredSize(new Dimension(380, 36));
        exitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        exitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        exitButton.addActionListener(e -> System.exit(0));
        form.add(exitButton);

        return form;
    }

    private JButton createChipButton(String text, String user, String pass) {
        JButton chip = new JButton(text);
        chip.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        boolean dark = UITheme.isDarkMode();
        chip.setBackground(dark ? new Color(30, 41, 59) : new Color(241, 245, 249));
        chip.setForeground(UITheme.PRIMARY_BLUE);
        chip.setFocusPainted(false);
        chip.setCursor(new Cursor(Cursor.HAND_CURSOR));
        chip.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(dark ? new Color(51, 65, 85) : new Color(203, 213, 225), 1, true),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        chip.addActionListener(e -> {
            usernameField.setText(user);
            passwordField.setText(pass);
            errorBanner.setVisible(false);
        });
        return chip;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Authenticating...");

        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                return authService.login(username, password, "127.0.0.1");
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    if (user != null) {
                        openDashboard(user);
                        dispose();
                    } else {
                        showError("Invalid username or password. Please verify credentials.");
                        passwordField.setText("");
                        passwordField.requestFocus();
                    }
                } catch (Exception ex) {
                    showError("Authentication error: " + ex.getMessage());
                } finally {
                    loginButton.setEnabled(true);
                    loginButton.setText("Sign In to Dashboard  →");
                }
            }
        }.execute();
    }

    private void showError(String msg) {
        errorLabel.setText("⚠️ " + msg);
        errorBanner.setBackground(UITheme.getDangerBg());
        errorBanner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.getDangerBorder(), 1, true),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        errorLabel.setForeground(UITheme.getDangerFg());
        errorBanner.setVisible(true);
        revalidate();
    }

    private void openDashboard(User user) {
        SwingUtilities.invokeLater(() -> {
            MainDashboard dashboard = new MainDashboard(user);
            dashboard.setVisible(true);
        });
    }

    public static void main(String[] args) {
        UITheme.initTheme();

        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
