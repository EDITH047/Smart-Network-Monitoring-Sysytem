package com.networkmonitor.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * PasswordUtil - Utility class for password hashing using SHA-256
 *
 * Used to:
 * - Hash passwords before storing in database
 * - Verify passwords during login
 */
public class PasswordUtil {

    /**
     * Hash a password using SHA-256 algorithm
     *
     * @param password Plain text password
     * @return Hashed password (hex string)
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            System.err.println("[PasswordUtil] SHA-256 algorithm not available: " + e.getMessage());
            throw new RuntimeException("Cannot hash password", e);
        }
    }

    /**
     * Verify a password by comparing hashes
     *
     * @param plainPassword Plain text password entered by user
     * @param hashedPassword Hashed password from database
     * @return true if passwords match, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        String hash = hashPassword(plainPassword);
        return hash.equals(hashedPassword);
    }

    /**
     * Generate a test hash (for demo purposes)
     * Default password for admin: "admin123"
     * Hashed value: 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
     *
     * @param args Not used
     */
    public static void main(String[] args) {
        String testPassword = "admin123";
        String hash = hashPassword(testPassword);
        System.out.println("Password: " + testPassword);
        System.out.println("Hash: " + hash);
    }
}
