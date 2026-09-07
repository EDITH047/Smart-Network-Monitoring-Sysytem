package com.networkmonitor.service;

import com.networkmonitor.dao.AuditLogDAO;
import com.networkmonitor.dao.UserDAO;
import com.networkmonitor.model.AuditLog;
import com.networkmonitor.model.User;
import com.networkmonitor.util.PasswordUtil;

/**
 * AuthService - Handles user authentication, authorization, and session management
 * Uses: UserDAO, PasswordUtil, AuditLogDAO
 */
public class AuthService {

    private static AuthService instance;
    private UserDAO userDAO = new UserDAO();
    private AuditLogDAO auditLogDAO = new AuditLogDAO();
    private User currentUser;

    // Private constructor for singleton
    private AuthService() {
    }

    /**
     * Get singleton instance
     */
    public static AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    /**
     * Authenticate user with username and password
     *
     * @param username Username
     * @param password Plain text password
     * @param ipAddress Client IP address
     * @return Authenticated User object if successful, null if failed
     */
    public User login(String username, String password, String ipAddress) {
        System.out.println("[AuthService] Login attempt: " + username + " from " + ipAddress);

        try {
            // Find user by username
            User user = userDAO.findByUsername(username);

            if (user == null) {
                // User not found
                logAuditEvent(0, "LOGIN_FAILED", "User not found: " + username, ipAddress);
                System.out.println("[AuthService] Login failed: user not found");
                return null;
            }

            // Check if user is active
            if (!user.isActive()) {
                logAuditEvent(user.getUserId(), "LOGIN_FAILED", "Account is deactivated", ipAddress);
                System.out.println("[AuthService] Login failed: account is deactivated");
                return null;
            }

            // Verify password
            if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                logAuditEvent(user.getUserId(), "LOGIN_FAILED", "Invalid password", ipAddress);
                System.out.println("[AuthService] Login failed: invalid password");
                return null;
            }

            // Update last login time
            userDAO.updateLastLogin(user.getUserId());

            // Set current user (session)
            this.currentUser = user;

            // Log successful login
            logAuditEvent(user.getUserId(), "LOGIN_SUCCESS", "Logged in successfully", ipAddress);
            System.out.println("[AuthService] Login successful: " + username + " (Role: " + user.getRole() + ")");

            return user;

        } catch (Exception e) {
            System.err.println("[AuthService] Login error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Logout current user
     */
    public void logout(String ipAddress) {
        if (currentUser != null) {
            logAuditEvent(currentUser.getUserId(), "LOGOUT", "Logged out", ipAddress);
            System.out.println("[AuthService] User logged out: " + currentUser.getUsername());
            currentUser = null;
        }
    }

    /**
     * Get currently logged-in user
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Check if current user has permission for an action
     *
     * @param action Action to check (e.g., "ADD_DEVICE", "DELETE_USER")
     * @return true if authorized, false otherwise
     */
    public boolean hasPermission(String action) {
        if (currentUser == null) {
            return false;
        }

        String role = currentUser.getRole();

        // ADMIN has all permissions
        if ("ADMIN".equals(role)) {
            return true;
        }

        // OPERATOR has most permissions except user management
        if ("OPERATOR".equals(role)) {
            return !action.equals("MANAGE_USERS") && !action.equals("DELETE_USER") && !action.equals("DEACTIVATE_USER");
        }

        // VIEWER is read-only
        if ("VIEWER".equals(role)) {
            return action.startsWith("VIEW_") || action.equals("VIEW_DASHBOARD") || action.equals("VIEW_REPORTS");
        }

        return false;
    }

    /**
     * Check specific role
     */
    public boolean isRole(String role) {
        return currentUser != null && currentUser.getRole().equals(role);
    }

    /**
     * Check if current user is admin
     */
    public boolean isAdmin() {
        return isRole("ADMIN");
    }

    /**
     * Check if current user is operator
     */
    public boolean isOperator() {
        return isRole("OPERATOR");
    }

    /**
     * Check if current user is viewer
     */
    public boolean isViewer() {
        return isRole("VIEWER");
    }

    /**
     * Register a new user (admin only)
     *
     * @param username Username
     * @param password Plain text password
     * @param fullName Full name
     * @param email Email address
     * @param role Role (ADMIN, OPERATOR, VIEWER)
     * @return true if successful, false otherwise
     */
    public boolean registerUser(String username, String password, String fullName, String email, String role) {
        // Only admin can register
        if (!isAdmin()) {
            System.out.println("[AuthService] Registration denied: admin only");
            return false;
        }

        // Check if username already exists
        if (userDAO.userExists(username)) {
            System.out.println("[AuthService] Registration failed: username already exists");
            return false;
        }

        // Hash password
        String passwordHash = PasswordUtil.hashPassword(password);

        // Create user
        User newUser = new User(username, passwordHash, fullName, email, role);
        boolean success = userDAO.insertUser(newUser);

        if (success) {
            logAuditEvent(currentUser.getUserId(), "USER_CREATED", "Created user: " + username + " (Role: " + role + ")", "");
            System.out.println("[AuthService] User registered: " + username);
        }

        return success;
    }

    /**
     * Change password for current user
     *
     * @param currentPassword Current password
     * @param newPassword New password
     * @return true if successful, false otherwise
     */
    public boolean changePassword(String currentPassword, String newPassword) {
        if (currentUser == null) {
            return false;
        }

        // Verify current password
        if (!PasswordUtil.verifyPassword(currentPassword, currentUser.getPasswordHash())) {
            System.out.println("[AuthService] Password change failed: incorrect current password");
            return false;
        }

        // Hash new password
        String newHash = PasswordUtil.hashPassword(newPassword);

        // Update in database
        boolean success = userDAO.updatePassword(currentUser.getUserId(), newHash);

        if (success) {
            currentUser.setPasswordHash(newHash);
            logAuditEvent(currentUser.getUserId(), "PASSWORD_CHANGED", "User changed password", "");
            System.out.println("[AuthService] Password changed for: " + currentUser.getUsername());
        }

        return success;
    }

    /**
     * Log audit event
     */
    private void logAuditEvent(int userId, String action, String details, String ipAddress) {
        try {
            AuditLog log = new AuditLog(userId, action, details, ipAddress);
            auditLogDAO.insertLog(log);
        } catch (Exception e) {
            System.err.println("[AuthService] Error logging audit event: " + e.getMessage());
        }
    }
}
