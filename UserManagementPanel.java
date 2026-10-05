package com.networkmonitor.ui;

import com.networkmonitor.dao.UserDAO;
import com.networkmonitor.model.User;
import com.networkmonitor.service.AuthService;
import com.networkmonitor.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class UserManagementPanel extends JPanel {

    private AuthService authService;
    private UserDAO userDAO;
    private User currentUser;

    private JTable userTable;
    private DefaultTableModel tableModel;

    public UserManagementPanel(User currentUser) {
        this.currentUser = currentUser;
        this.authService = AuthService.getInstance();
        this.userDAO = new UserDAO();
        initializeUI();
        loadUsers();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("👥 User Management");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Buttons Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton addUserBtn = new JButton("➕ Add User");
        UITheme.styleSuccessButton(addUserBtn);
        addUserBtn.addActionListener(e -> showAddUserDialog());

        JButton toggleActiveBtn = new JButton("⏸️ Toggle Active");
        UITheme.styleNeutralButton(toggleActiveBtn);
        toggleActiveBtn.addActionListener(e -> toggleUserActive());

        JButton resetPassBtn = new JButton("🔑 Reset Password");
        UITheme.stylePrimaryButton(resetPassBtn);
        resetPassBtn.addActionListener(e -> resetUserPassword());

        JButton refreshBtn = new JButton("🔄 Refresh");
        UITheme.styleNeutralButton(refreshBtn);
        refreshBtn.addActionListener(e -> loadUsers());

        if (currentUser.getRole().equals("ADMIN")) {
            btnPanel.add(addUserBtn);
            btnPanel.add(toggleActiveBtn);
            btnPanel.add(resetPassBtn);
        }
        btnPanel.add(refreshBtn);

        // Table
        JPanel tablePanel = new JPanel(new BorderLayout());
        UITheme.styleCard(tablePanel);

        tableModel = new DefaultTableModel(new String[]{"ID", "Username", "Full Name", "Email", "Role", "Status", "Last Login"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        userTable = new JTable(tableModel);
        userTable.setAutoCreateRowSorter(true);
        UITheme.styleTable(userTable);

        btnPanel.add(UITheme.createSearchBar(userTable), 0); // Add search before buttons
        headerPanel.add(btnPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        add(tablePanel, BorderLayout.CENTER);
    }

    private void loadUsers() {
        tableModel.setRowCount(0);
        List<User> users = userDAO.getAllUsers();
        for (User u : users) {
            tableModel.addRow(new Object[]{
                    u.getUserId(),
                    u.getUsername(),
                    u.getFullName(),
                    u.getEmail(),
                    u.getRole(),
                    u.isActive() ? "Active" : "Inactive",
                    u.getLastLogin() != null ? u.getLastLogin().toString() : "Never"
            });
        }
    }

    private void showAddUserDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add New User", true);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        p.setBackground(UITheme.CARD_BG);

        JTextField txtUsername = new JTextField();
        JPasswordField txtPassword = new JPasswordField();
        JTextField txtFullName = new JTextField();
        JTextField txtEmail = new JTextField();
        JComboBox<String> cbRole = new JComboBox<>(new String[]{"ADMIN", "OPERATOR", "VIEWER"});

        UITheme.styleTextField(txtUsername);
        UITheme.styleTextField(txtPassword);
        UITheme.styleTextField(txtFullName);
        UITheme.styleTextField(txtEmail);

        p.add(createFieldRow("Username", txtUsername));
        p.add(createFieldRow("Password", txtPassword));
        p.add(createFieldRow("Full Name", txtFullName));
        p.add(createFieldRow("Email", txtEmail));
        p.add(createFieldRow("Role", cbRole));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRow.setOpaque(false);

        JButton saveBtn = new JButton("Create User");
        UITheme.stylePrimaryButton(saveBtn);
        saveBtn.addActionListener(e -> {
            String u = txtUsername.getText().trim();
            String pwd = new String(txtPassword.getPassword());
            String n = txtFullName.getText().trim();
            String em = txtEmail.getText().trim();
            String r = (String) cbRole.getSelectedItem();

            if (u.isEmpty() || pwd.isEmpty() || n.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill required fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (authService.registerUser(u, pwd, n, em, r)) {
                loadUsers();
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Registration failed (User might exist)", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelBtn = new JButton("Cancel");
        UITheme.styleNeutralButton(cancelBtn);
        cancelBtn.addActionListener(e -> dialog.dispose());

        btnRow.add(saveBtn);
        btnRow.add(cancelBtn);
        p.add(btnRow);

        dialog.add(p);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(350, dialog.getPreferredSize().height));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void toggleUserActive() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a user to toggle");
            return;
        }
        int userId = (int) tableModel.getValueAt(row, 0);
        String currentStatus = (String) tableModel.getValueAt(row, 5);
        boolean isActive = "Active".equals(currentStatus);
        
        userDAO.setUserActive(userId, !isActive);
        loadUsers();
    }

    private void resetUserPassword() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a user");
            return;
        }
        int userId = (int) tableModel.getValueAt(row, 0);
        String newPass = JOptionPane.showInputDialog(this, "Enter new password:");
        if (newPass != null && !newPass.trim().isEmpty()) {
            userDAO.updatePassword(userId, com.networkmonitor.util.PasswordUtil.hashPassword(newPass));
            JOptionPane.showMessageDialog(this, "Password reset successful");
        }
    }

    private JPanel createFieldRow(String labelText, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UITheme.FONT_BODY_BOLD);
        p.add(lbl, BorderLayout.NORTH);
        comp.setPreferredSize(new Dimension(0, 32));
        p.add(comp, BorderLayout.CENTER);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        return p;
    }
}
