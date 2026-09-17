package com.networkmonitor.ui;

import com.networkmonitor.model.FirewallRule;
import com.networkmonitor.model.User;
import com.networkmonitor.service.FirewallService;
import com.networkmonitor.util.UITheme;
import com.networkmonitor.util.ValidationUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

/**
 * FirewallPanel - Priority-ordered rule engine GUI
 */
public class FirewallPanel extends JPanel {

    private User currentUser;
    private FirewallService firewallService;

    private JTable rulesTable;
    private DefaultTableModel tableModel;

    private JButton addRuleBtn;
    private JButton editRuleBtn;
    private JButton deleteRuleBtn;
    private JButton toggleBtn;
    private JButton refreshBtn;

    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_SRC = 2;
    private static final int COL_DEST = 3;
    private static final int COL_PORT = 4;
    private static final int COL_PROTO = 5;
    private static final int COL_ACTION = 6;
    private static final int COL_PRIORITY = 7;
    private static final int COL_ACTIVE = 8;

    public FirewallPanel(User currentUser) {
        this.currentUser = currentUser;
        this.firewallService = FirewallService.getInstance();
        initializeUI();
        loadRules();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_CANVAS);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Top Panel: Header and Controls
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("🔥 Advanced Firewall Configuration");
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        topPanel.add(titleLabel, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        addRuleBtn = new JButton("➕ New Rule");
        UITheme.styleSuccessButton(addRuleBtn);
        addRuleBtn.addActionListener(e -> showAddRuleDialog());
        addRuleBtn.setEnabled(isOperatorOrAdmin());
        btnPanel.add(addRuleBtn);

        editRuleBtn = new JButton("✏️ Edit");
        UITheme.stylePrimaryButton(editRuleBtn);
        editRuleBtn.addActionListener(e -> showEditRuleDialog());
        editRuleBtn.setEnabled(isOperatorOrAdmin());
        btnPanel.add(editRuleBtn);

        toggleBtn = new JButton("⏸️ Toggle Active");
        UITheme.styleNeutralButton(toggleBtn);
        toggleBtn.addActionListener(e -> toggleSelectedRule());
        toggleBtn.setEnabled(isOperatorOrAdmin());
        btnPanel.add(toggleBtn);

        deleteRuleBtn = new JButton("🗑️ Delete");
        UITheme.styleDangerButton(deleteRuleBtn);
        deleteRuleBtn.addActionListener(e -> deleteSelectedRule());
        deleteRuleBtn.setEnabled(isOperatorOrAdmin());
        btnPanel.add(deleteRuleBtn);

        refreshBtn = new JButton("🔄 Refresh");
        UITheme.styleNeutralButton(refreshBtn);
        refreshBtn.addActionListener(e -> loadRules());
        btnPanel.add(refreshBtn);

        topPanel.add(btnPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Center Panel: Rules Table
        JPanel centerPanel = new JPanel(new BorderLayout());
        UITheme.styleCard(centerPanel);

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tableModel.setColumnIdentifiers(new String[]{
            "Rule ID", "Name", "Source IP", "Destination IP", "Port", "Protocol", "Action", "Priority", "Status"
        });

        rulesTable = new JTable(tableModel);
        UITheme.styleTable(rulesTable);

        // Column widths
        rulesTable.getColumnModel().getColumn(COL_ID).setPreferredWidth(50);
        rulesTable.getColumnModel().getColumn(COL_NAME).setPreferredWidth(140);
        rulesTable.getColumnModel().getColumn(COL_SRC).setPreferredWidth(100);
        rulesTable.getColumnModel().getColumn(COL_DEST).setPreferredWidth(100);
        rulesTable.getColumnModel().getColumn(COL_PORT).setPreferredWidth(60);
        rulesTable.getColumnModel().getColumn(COL_PROTO).setPreferredWidth(70);
        rulesTable.getColumnModel().getColumn(COL_ACTION).setPreferredWidth(100);
        rulesTable.getColumnModel().getColumn(COL_PRIORITY).setPreferredWidth(60);
        rulesTable.getColumnModel().getColumn(COL_ACTIVE).setPreferredWidth(80);

        // Renderers
        rulesTable.getColumnModel().getColumn(COL_ACTION).setCellRenderer(new ActionCellRenderer());
        rulesTable.getColumnModel().getColumn(COL_ACTIVE).setCellRenderer(new StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(rulesTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setOpaque(false);
        JLabel hintLabel = new JLabel("Lower priority number = Higher precedence. Rules are evaluated top-to-bottom.");
        hintLabel.setFont(UITheme.FONT_SMALL);
        hintLabel.setForeground(UITheme.TEXT_MUTED);
        bottomPanel.add(hintLabel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadRules() {
        try {
            tableModel.setRowCount(0);
            List<FirewallRule> rules = firewallService.getAllRules();
            for (FirewallRule rule : rules) {
                tableModel.addRow(new Object[]{
                    rule.getRuleId(),
                    rule.getRuleName(),
                    rule.getSourceIp() == null || rule.getSourceIp().isEmpty() ? "ANY" : rule.getSourceIp(),
                    rule.getDestIp() == null || rule.getDestIp().isEmpty() ? "ANY" : rule.getDestIp(),
                    rule.getPort() == 0 ? "ANY" : rule.getPort(),
                    rule.getProtocol(),
                    rule.getAction(),
                    rule.getPriority(),
                    rule.isActive() ? "ACTIVE" : "DISABLED"
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading firewall rules: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAddRuleDialog() {
        showRuleDialog(null);
    }

    private void showEditRuleDialog() {
        int row = rulesTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a rule to edit.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int ruleId = (Integer) tableModel.getValueAt(row, COL_ID);
        // We get it fresh from DB logic inside service/DAO if needed, but for simplicity we instantiate a dummy or fetch it
        // A standard approach using DAO
        com.networkmonitor.dao.FirewallRuleDAO dao = new com.networkmonitor.dao.FirewallRuleDAO();
        FirewallRule rule = dao.getRuleById(ruleId);
        if (rule != null) {
            showRuleDialog(rule);
        }
    }

    private void showRuleDialog(FirewallRule existingRule) {
        boolean isEdit = (existingRule != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "Edit Firewall Rule" : "Create Firewall Rule", true);
        dialog.setSize(450, 500);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        mainPanel.setBackground(UITheme.CARD_BG);

        JTextField nameField = new JTextField(isEdit ? existingRule.getRuleName() : "");
        JTextField srcField = new JTextField(isEdit ? (existingRule.getSourceIp() != null ? existingRule.getSourceIp() : "") : "");
        JTextField destField = new JTextField(isEdit ? (existingRule.getDestIp() != null ? existingRule.getDestIp() : "") : "");
        JTextField portField = new JTextField(isEdit ? (existingRule.getPort() > 0 ? String.valueOf(existingRule.getPort()) : "") : "");

        JComboBox<String> protoCombo = new JComboBox<>(new String[]{"ALL", "TCP", "UDP", "ICMP"});
        if (isEdit) protoCombo.setSelectedItem(existingRule.getProtocol());

        JComboBox<String> actionCombo = new JComboBox<>(new String[]{"ALLOW", "BLOCK", "RATE_LIMIT"});
        if (isEdit) actionCombo.setSelectedItem(existingRule.getAction());

        JTextField priorityField = new JTextField(isEdit ? String.valueOf(existingRule.getPriority()) : "100");

        UITheme.styleTextField(nameField);
        UITheme.styleTextField(srcField);
        UITheme.styleTextField(destField);
        UITheme.styleTextField(portField);
        UITheme.styleTextField(priorityField);

        mainPanel.add(createFieldRow("Rule Name *", nameField, "e.g., Block Suspicious Subnet"));
        mainPanel.add(createFieldRow("Source IP (Leave empty for ANY)", srcField, "e.g., 192.168.1.50"));
        mainPanel.add(createFieldRow("Dest IP (Leave empty for ANY)", destField, "e.g., 10.0.0.5"));
        mainPanel.add(createFieldRow("Port (Leave empty for ANY)", portField, "e.g., 80, 443"));
        mainPanel.add(createFieldRow("Protocol", protoCombo, ""));
        mainPanel.add(createFieldRow("Action", actionCombo, ""));
        mainPanel.add(createFieldRow("Priority (Lower = Higher Precedence)", priorityField, "e.g., 10"));

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.DANGER_RED);
        mainPanel.add(errorLabel);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        JButton saveBtn = new JButton(isEdit ? "Update Rule" : "Save Rule");
        UITheme.stylePrimaryButton(saveBtn);

        JButton cancelBtn = new JButton("Cancel");
        UITheme.styleNeutralButton(cancelBtn);
        cancelBtn.addActionListener(e -> dialog.dispose());

        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String src = srcField.getText().trim();
            String dest = destField.getText().trim();
            String portStr = portField.getText().trim();
            String proto = (String) protoCombo.getSelectedItem();
            String action = (String) actionCombo.getSelectedItem();
            String prioStr = priorityField.getText().trim();

            if (name.isEmpty()) {
                errorLabel.setText("Rule Name is required");
                return;
            }

            int port = 0;
            if (!portStr.isEmpty()) {
                try {
                    port = Integer.parseInt(portStr);
                    if (!ValidationUtil.isValidPort(port)) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    errorLabel.setText("Invalid Port number (1-65535)");
                    return;
                }
            }

            int priority = 100;
            try { priority = Integer.parseInt(prioStr); } catch (Exception ignored) {}

            if (isEdit) {
                existingRule.setRuleName(name);
                existingRule.setSourceIp(src);
                existingRule.setDestIp(dest);
                existingRule.setPort(port);
                existingRule.setProtocol(proto);
                existingRule.setAction(action);
                existingRule.setPriority(priority);
                if (firewallService.updateRule(existingRule)) {
                    loadRules();
                    dialog.dispose();
                } else {
                    errorLabel.setText("Failed to update rule");
                }
            } else {
                if (firewallService.addRule(name, src, dest, port, proto, action, priority, currentUser.getUserId())) {
                    loadRules();
                    dialog.dispose();
                } else {
                    errorLabel.setText("Failed to save. Rule name exists or validation failed.");
                }
            }
        });

        btnRow.add(saveBtn);
        btnRow.add(cancelBtn);
        mainPanel.add(btnRow);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void toggleSelectedRule() {
        int row = rulesTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a rule to toggle.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int ruleId = (Integer) tableModel.getValueAt(row, COL_ID);
        if (firewallService.toggleRuleActive(ruleId)) {
            loadRules();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to toggle rule state.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedRule() {
        int row = rulesTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a rule to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int ruleId = (Integer) tableModel.getValueAt(row, COL_ID);
        String ruleName = (String) tableModel.getValueAt(row, COL_NAME);

        int confirm = JOptionPane.showConfirmDialog(this, "Delete firewall rule '" + ruleName + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (firewallService.deleteRule(ruleId)) {
                loadRules();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete rule.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel createFieldRow(String labelText, JComponent comp, String hint) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JPanel lblPnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lblPnl.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UITheme.FONT_BODY_BOLD);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        lblPnl.add(lbl);

        if (!hint.isEmpty()) {
            JLabel hnt = new JLabel("  (" + hint + ")");
            hnt.setFont(UITheme.FONT_SMALL);
            hnt.setForeground(UITheme.TEXT_MUTED);
            lblPnl.add(hnt);
        }

        p.add(lblPnl);
        p.add(Box.createVerticalStrut(3));
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        p.add(comp);
        p.add(Box.createVerticalStrut(8));
        return p;
    }

    private boolean isOperatorOrAdmin() {
        String role = currentUser.getRole();
        return "ADMIN".equals(role) || "OPERATOR".equals(role);
    }

    private static class ActionCellRenderer extends JLabel implements TableCellRenderer {
        ActionCellRenderer() { setOpaque(true); setHorizontalAlignment(CENTER); setFont(UITheme.FONT_BODY_BOLD); }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String act = (String) value;
            setText(act);
            if ("ALLOW".equals(act)) {
                setBackground(new Color(220, 252, 231));
                setForeground(UITheme.SUCCESS_GREEN);
            } else if ("BLOCK".equals(act)) {
                setBackground(new Color(254, 226, 226));
                setForeground(UITheme.DANGER_RED);
            } else if ("RATE_LIMIT".equals(act)) {
                setBackground(new Color(254, 243, 199));
                setForeground(UITheme.WARNING_ORANGE);
            }
            if (isSelected) { setBackground(UITheme.PRIMARY_BLUE); setForeground(UITheme.TEXT_LIGHT); }
            return this;
        }
    }

    private static class StatusCellRenderer extends JLabel implements TableCellRenderer {
        StatusCellRenderer() { setOpaque(true); setHorizontalAlignment(CENTER); setFont(UITheme.FONT_BODY_BOLD); }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String stat = (String) value;
            setText(stat);
            if ("ACTIVE".equals(stat)) {
                setBackground(new Color(220, 252, 231));
                setForeground(UITheme.SUCCESS_GREEN);
            } else {
                setBackground(new Color(241, 245, 249));
                setForeground(UITheme.TEXT_MUTED);
            }
            if (isSelected) { setBackground(UITheme.PRIMARY_BLUE); setForeground(UITheme.TEXT_LIGHT); }
            return this;
        }
    }
}
