package com.networkmonitor.util;

import java.util.regex.Pattern;

/**
 * ValidationUtil - Utility class for input validation
 *
 * Used to:
 * - Validate IP addresses (IPv4)
 * - Validate email addresses
 * - Validate port numbers
 * - Sanitize inputs
 */
public class ValidationUtil {

    // Regex patterns
    private static final String IPV4_REGEX = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final String MAC_REGEX = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";
    private static final String HOSTNAME_REGEX = "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)*[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?$";

    private static final Pattern IPV4_PATTERN = Pattern.compile(IPV4_REGEX);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private static final Pattern MAC_PATTERN = Pattern.compile(MAC_REGEX);
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(HOSTNAME_REGEX);

    /**
     * Validate IPv4 address
     *
     * @param ip IP address string
     * @return true if valid IPv4 address
     */
    public static boolean isValidIPv4(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip).matches();
    }

    /**
     * Validate email address
     *
     * @param email Email address string
     * @return true if valid email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate MAC address
     *
     * @param mac MAC address string (format: AA:BB:CC:DD:EE:FF or AA-BB-CC-DD-EE-FF)
     * @return true if valid MAC address
     */
    public static boolean isValidMac(String mac) {
        if (mac == null || mac.trim().isEmpty()) {
            return false;
        }
        return MAC_PATTERN.matcher(mac).matches();
    }

    /**
     * Validate hostname
     *
     * @param hostname Hostname string
     * @return true if valid hostname
     */
    public static boolean isValidHostname(String hostname) {
        if (hostname == null || hostname.trim().isEmpty()) {
            return false;
        }
        return HOSTNAME_PATTERN.matcher(hostname).matches();
    }

    /**
     * Validate port number
     *
     * @param port Port number (1-65535)
     * @return true if valid port
     */
    public static boolean isValidPort(int port) {
        return port > 0 && port <= 65535;
    }

    /**
     * Validate username (alphanumeric, 3-50 characters)
     *
     * @param username Username string
     * @return true if valid username
     */
    public static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9_.-]{3,50}$");
    }

    /**
     * Validate password (minimum 6 characters)
     *
     * @param password Password string
     * @return true if valid password
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        return password.length() >= 6;
    }

    /**
     * Check if string is empty or null
     *
     * @param str String to check
     * @return true if empty or null
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Sanitize input string (remove special characters that could cause SQL injection)
     * Note: This is a basic sanitizer. Always use PreparedStatements for SQL!
     *
     * @param input Input string
     * @return Sanitized string
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[;'\"\\\\]", "");
    }

    /**
     * Validate latitude (-90 to 90)
     *
     * @param latitude Latitude value
     * @return true if valid latitude
     */
    public static boolean isValidLatitude(double latitude) {
        return latitude >= -90 && latitude <= 90;
    }

    /**
     * Validate longitude (-180 to 180)
     *
     * @param longitude Longitude value
     * @return true if valid longitude
     */
    public static boolean isValidLongitude(double longitude) {
        return longitude >= -180 && longitude <= 180;
    }

    /**
     * Test the validation methods
     */
    public static void main(String[] args) {
        System.out.println("=== ValidationUtil Test ===\n");

        // Test IPv4
        System.out.println("IPv4 Tests:");
        System.out.println("192.168.1.1 -> " + isValidIPv4("192.168.1.1"));
        System.out.println("256.1.1.1 -> " + isValidIPv4("256.1.1.1"));
        System.out.println("192.168.1 -> " + isValidIPv4("192.168.1"));

        // Test Email
        System.out.println("\nEmail Tests:");
        System.out.println("admin@network.local -> " + isValidEmail("admin@network.local"));
        System.out.println("invalid.email -> " + isValidEmail("invalid.email"));

        // Test MAC
        System.out.println("\nMAC Tests:");
        System.out.println("AA:BB:CC:DD:EE:FF -> " + isValidMac("AA:BB:CC:DD:EE:FF"));
        System.out.println("AA-BB-CC-DD-EE-FF -> " + isValidMac("AA-BB-CC-DD-EE-FF"));
        System.out.println("INVALID -> " + isValidMac("INVALID"));

        // Test Port
        System.out.println("\nPort Tests:");
        System.out.println("8080 -> " + isValidPort(8080));
        System.out.println("70000 -> " + isValidPort(70000));
        System.out.println("0 -> " + isValidPort(0));

        // Test Username
        System.out.println("\nUsername Tests:");
        System.out.println("admin -> " + isValidUsername("admin"));
        System.out.println("ab -> " + isValidUsername("ab"));
        System.out.println("user_123 -> " + isValidUsername("user_123"));
    }
}
