package com.networkmonitor.dao;

import com.networkmonitor.config.DatabaseConfig;
import com.networkmonitor.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO - Data Access Object for User table
 * Handles all JDBC operations related to users
 */
public class UserDAO {

    /**
     * Insert a new user into the database
     *
     * @param user User object to insert
     * @return true if successful, false otherwise
     */
    public boolean insertUser(User user) {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, role, is_active, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getRole());
            ps.setBoolean(6, user.isActive());

            int rowsInserted = ps.executeUpdate();
            System.out.println("[UserDAO] User inserted: " + user.getUsername());
            return rowsInserted > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error inserting user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Find a user by username
     *
     * @param username Username to search for
     * @return User object if found, null otherwise
     */
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error finding user by username: " + e.getMessage());
        }

        return null;
    }

    /**
     * Find a user by user ID
     *
     * @param userId User ID to search for
     * @return User object if found, null otherwise
     */
    public User findById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error finding user by ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get all users from the database
     *
     * @return List of all User objects
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }

            System.out.println("[UserDAO] Retrieved " + users.size() + " users");

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error retrieving all users: " + e.getMessage());
        }

        return users;
    }

    /**
     * Get all active users
     *
     * @return List of active User objects
     */
    public List<User> getActiveUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE is_active = TRUE ORDER BY username";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error retrieving active users: " + e.getMessage());
        }

        return users;
    }

    /**
     * Update a user's information
     *
     * @param user User object with updated information
     * @return true if successful, false otherwise
     */
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET full_name = ?, email = ?, role = ?, is_active = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getRole());
            ps.setBoolean(4, user.isActive());
            ps.setInt(5, user.getUserId());

            int rowsUpdated = ps.executeUpdate();
            System.out.println("[UserDAO] User updated: " + user.getUsername());
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error updating user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update user's last login timestamp
     *
     * @param userId User ID
     * @return true if successful, false otherwise
     */
    public boolean updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = NOW() WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            int rowsUpdated = ps.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error updating last login: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update user's password
     *
     * @param userId User ID
     * @param newPasswordHash New password hash
     * @return true if successful, false otherwise
     */
    public boolean updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);

            int rowsUpdated = ps.executeUpdate();
            System.out.println("[UserDAO] Password updated for user ID: " + userId);
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error updating password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deactivate a user account
     *
     * @param userId User ID to deactivate
     * @return true if successful, false otherwise
     */
    public boolean deactivateUser(int userId) {
        String sql = "UPDATE users SET is_active = FALSE WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            int rowsUpdated = ps.executeUpdate();
            System.out.println("[UserDAO] User deactivated: " + userId);
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error deactivating user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Activate a user account
     *
     * @param userId User ID to activate
     * @return true if successful, false otherwise
     */
    public boolean activateUser(int userId) {
        String sql = "UPDATE users SET is_active = TRUE WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            int rowsUpdated = ps.executeUpdate();
            System.out.println("[UserDAO] User activated: " + userId);
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error activating user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete a user from the database
     *
     * @param userId User ID to delete
     * @return true if successful, false otherwise
     */
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            int rowsDeleted = ps.executeUpdate();
            System.out.println("[UserDAO] User deleted: " + userId);
            return rowsDeleted > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error deleting user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if username already exists
     *
     * @param username Username to check
     * @return true if exists, false otherwise
     */
    public boolean userExists(String username) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error checking if user exists: " + e.getMessage());
        }

        return false;
    }

    /**
     * Get count of users with a specific role
     *
     * @param role Role to count (ADMIN, OPERATOR, VIEWER)
     * @return Count of users with that role
     */
    public int getUserCountByRole(String role) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE role = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error counting users by role: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Helper method to map ResultSet row to User object
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setRole(rs.getString("role"));
        user.setActive(rs.getBoolean("is_active"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setLastLogin(rs.getTimestamp("last_login"));
        return user;
    }
}
