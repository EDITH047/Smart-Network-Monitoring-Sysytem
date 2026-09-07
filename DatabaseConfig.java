package com.networkmonitor.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConfig - Singleton class to manage JDBC connection to MySQL
 *
 * All DAO classes use this class to get a database connection.
 * Connection pooling pattern: maintains a single connection instance.
 */
public class DatabaseConfig {

    // MySQL connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/network_monitor_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";

    // Singleton instance
    private static Connection connection;

    /**
     * Private constructor to prevent instantiation
     */
    private DatabaseConfig() {
    }

    /**
     * Get database connection (singleton pattern)
     *
     * @return Connection object to the database
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Load MySQL JDBC driver
            Class.forName(DB_DRIVER);

            // Check if connection is null or closed
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                System.out.println("[DatabaseConfig] New connection established");
            }

            return connection;

        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConfig] MySQL Driver not found: " + e.getMessage());
            throw new SQLException("MySQL JDBC Driver not found", e);

        } catch (SQLException e) {
            System.err.println("[DatabaseConfig] Connection failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Close the database connection (call on application shutdown)
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("[DatabaseConfig] Connection closed");
            } catch (SQLException e) {
                System.err.println("[DatabaseConfig] Error closing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Check if connection is active
     *
     * @return true if connected and valid, false otherwise
     */
    public static boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Reconnect to database (forces a new connection)
     *
     * @throws SQLException if reconnection fails
     */
    public static void reconnect() throws SQLException {
        closeConnection();
        connection = null;
        getConnection();
    }
}
