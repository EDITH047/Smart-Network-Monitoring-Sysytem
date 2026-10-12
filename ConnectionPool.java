package com.networkmonitor.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConnectionPool {
    private String url;
    private String user;
    private String password;
    private List<Connection> connectionPool;
    private List<Connection> usedConnections = new ArrayList<>();
    private static int INITIAL_POOL_SIZE = 10;
    private static int MAX_POOL_SIZE = 20;

    public ConnectionPool(String url, String user, String password) throws SQLException {
        this.url = url;
        this.user = user;
        this.password = password;
        this.connectionPool = new ArrayList<>(INITIAL_POOL_SIZE);
        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
            connectionPool.add(createConnection());
        }
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public synchronized Connection getConnection() throws SQLException {
        long startTime = System.currentTimeMillis();
        long maxWaitMs = 10000; // Wait up to 10 seconds for an available connection

        while (connectionPool.isEmpty() && usedConnections.size() >= MAX_POOL_SIZE) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= maxWaitMs) {
                throw new SQLException("Connection pool exhausted: timed out after " + maxWaitMs + "ms waiting for an available connection.");
            }
            try {
                wait(Math.min(1000, maxWaitMs - elapsed));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Thread interrupted while waiting for database connection", e);
            }
        }

        Connection connection;
        if (!connectionPool.isEmpty()) {
            connection = connectionPool.remove(connectionPool.size() - 1);
        } else {
            connection = createConnection();
        }

        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                connection = createConnection();
            }
        } catch (Exception e) {
            connection = createConnection();
        }

        usedConnections.add(connection);
        return connection;
    }

    public synchronized boolean releaseConnection(Connection connection) {
        if (connection != null) {
            usedConnections.remove(connection);
            try {
                if (!connection.isClosed()) {
                    if (!connection.getAutoCommit()) {
                        connection.setAutoCommit(true);
                    }
                    if (!connectionPool.contains(connection)) {
                        connectionPool.add(connection);
                    }
                }
            } catch (SQLException e) {
                // If checking or resetting connection fails, do not return to pool
                try { connection.close(); } catch (Exception ignored) {}
            }
            notifyAll();
            return true;
        }
        return false;
    }

    public synchronized int getPoolSize() {
        return connectionPool.size();
    }

    public synchronized int getUsedConnectionCount() {
        return usedConnections.size();
    }

    public synchronized void shutdown() throws SQLException {
        for (Connection c : connectionPool) {
            try { if (c != null && !c.isClosed()) c.close(); } catch (Exception ignored) {}
        }
        for (Connection c : usedConnections) {
            try { if (c != null && !c.isClosed()) c.close(); } catch (Exception ignored) {}
        }
        connectionPool.clear();
        usedConnections.clear();
        notifyAll();
    }
}
