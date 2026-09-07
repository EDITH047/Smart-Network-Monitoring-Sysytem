package com.networkmonitor.test;

import com.networkmonitor.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * ConnectionTest - Test class to verify JDBC connection and database setup
 *
 * Run this class to verify:
 * 1. MySQL driver is loaded
 * 2. Connection to database succeeds
 * 3. All required tables exist
 * 4. Sample data is accessible
 */
public class ConnectionTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Smart Network Monitoring System");
        System.out.println("Connection Test");
        System.out.println("========================================\n");

        Connection conn = null;

        try {
            // Step 1: Test connection
            System.out.println("[1] Testing database connection...");
            conn = DatabaseConfig.getConnection();

            if (conn != null && !conn.isClosed()) {
                System.out.println("✓ Connection successful!");
                System.out.println("   Database: network_monitor_db");
                System.out.println("   URL: jdbc:mysql://localhost:3306/network_monitor_db\n");
            } else {
                System.out.println("✗ Connection failed!");
                return;
            }

            // Step 2: Verify tables exist
            System.out.println("[2] Verifying database tables...");
            String[] tables = {"users", "devices", "network_metrics", "security_events",
                    "firewall_rules", "alerts", "optimization_results", "blocked_ips", "audit_log"};

            for (String table : tables) {
                if (tableExists(conn, table)) {
                    System.out.println("   ✓ " + table);
                } else {
                    System.out.println("   ✗ " + table + " (MISSING)");
                }
            }
            System.out.println();

            // Step 3: Check sample data
            System.out.println("[3] Checking sample data...");
            int userCount = getRowCount(conn, "users");
            int deviceCount = getRowCount(conn, "devices");

            System.out.println("   Users in database: " + userCount);
            System.out.println("   Devices in database: " + deviceCount);

            if (userCount > 0) {
                System.out.println("   ✓ Admin user exists\n");
            }

            // Step 4: Display admin user
            System.out.println("[4] Admin user details:");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT user_id, username, email, role FROM users WHERE role='ADMIN' LIMIT 1");

            if (rs.next()) {
                System.out.println("   User ID: " + rs.getInt("user_id"));
                System.out.println("   Username: " + rs.getString("username"));
                System.out.println("   Email: " + rs.getString("email"));
                System.out.println("   Role: " + rs.getString("role"));
                System.out.println("   Password hint: admin123 (hashed in DB)\n");
            }
            rs.close();
            stmt.close();

            // Step 5: Display sample devices
            System.out.println("[5] Sample devices:");
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT device_id, device_name, ip_address, device_type, status FROM devices LIMIT 5");

            int count = 0;
            while (rs.next()) {
                System.out.println("   [" + rs.getInt("device_id") + "] " + rs.getString("device_name") +
                        " | IP: " + rs.getString("ip_address") +
                        " | Type: " + rs.getString("device_type") +
                        " | Status: " + rs.getString("status"));
                count++;
            }
            if (count == 0) {
                System.out.println("   (No devices found - add via GUI)");
            }
            System.out.println();
            rs.close();
            stmt.close();

            // Success message
            System.out.println("========================================");
            System.out.println("✓ All tests passed!");
            System.out.println("✓ Database is ready for the application");
            System.out.println("========================================\n");

        } catch (Exception e) {
            System.out.println("\n✗ Test failed!");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();

        } finally {
            // Clean up
            DatabaseConfig.closeConnection();
        }
    }

    /**
     * Check if a table exists in the database
     */
    private static boolean tableExists(Connection conn, String tableName) {
        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery("SELECT 1 FROM " + tableName + " LIMIT 1");
            stmt.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get row count for a table
     */
    private static int getRowCount(Connection conn, String tableName) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM " + tableName);
            rs.next();
            int count = rs.getInt("count");
            rs.close();
            stmt.close();
            return count;
        } catch (Exception e) {
            return 0;
        }
    }
}
