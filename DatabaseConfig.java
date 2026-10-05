package com.networkmonitor.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.lang.reflect.Proxy;

/**
 * DatabaseConfig - Singleton class to manage JDBC connection to MySQL
 * Uses a ConnectionPool for thread-safe concurrency and high efficiency.
 */
public class DatabaseConfig {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/network_monitor_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";

    private static volatile ConnectionPool pool;

    private DatabaseConfig() {}

    /**
     * Lazy-init and retrieve the ConnectionPool.
     * Retries if previous attempts failed during startup.
     */
    public static synchronized ConnectionPool getPool() throws SQLException {
        if (pool == null) {
            try {
                Class.forName(DB_DRIVER);
                pool = new ConnectionPool(DB_URL, DB_USER, DB_PASSWORD);
                System.out.println("[DatabaseConfig] Connection pool initialized successfully.");
            } catch (Exception e) {
                System.err.println("[DatabaseConfig] Failed to initialize connection pool: " + e.getMessage());
                throw new SQLException("Connection pool is not initialized: " + e.getMessage(), e);
            }
        }
        return pool;
    }

    /**
     * Get database connection from the pool.
     * Returns a proxy that intercepts close() and releases it back to the pool.
     */
    public static Connection getConnection() throws SQLException {
        ConnectionPool currentPool = getPool();
        Connection realConnection = currentPool.getConnection();
        
        return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class[]{Connection.class},
            (proxy, method, args) -> {
                if ("close".equals(method.getName())) {
                    currentPool.releaseConnection(realConnection);
                    return null;
                }
                try {
                    return method.invoke(realConnection, args);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause(); // Unwrap SQL exceptions
                }
            }
        );
    }
    
    /**
     * Gracefully shutdown the connection pool
     */
    public static synchronized void shutdown() {
        if (pool != null) {
            try {
                pool.shutdown();
                pool = null;
                System.out.println("[DatabaseConfig] Connection pool shutdown successfully.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
