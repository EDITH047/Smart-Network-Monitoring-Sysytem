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

    // Singleton instances
    private static Connection realConnection;
    private static Connection proxyConnection;

    /**
     * Private constructor to prevent instantiation
     */
    private DatabaseConfig() {
    }

    /**
     * Get database connection (singleton pattern)
     * Returns a proxy that suppresses close() calls from try-with-resources blocks.
     *
     * @return Connection object to the database
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Load MySQL JDBC driver
            Class.forName(DB_DRIVER);

            // Check if connection is null or closed
            if (realConnection == null || realConnection.isClosed()) {
                realConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                
                // Create a proxy to ignore close() calls from DAOs
                proxyConnection = (Connection) java.lang.reflect.Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("close".equals(method.getName())) {
                            return null; // Ignore close() to keep singleton alive
                        }
                        try {
                            return method.invoke(realConnection, args);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            throw e.getCause(); // Unwrap SQL exceptions
                        }
                    }
                );
                
                System.out.println("[DatabaseConfig] New connection established");
            }

            return proxyConnection;

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
        if (realConnection != null) {
            try {
                realConnection.close();
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
            return realConnection != null && !realConnection.isClosed();
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
        realConnection = null;
        proxyConnection = null;
        getConnection();
    }
}
