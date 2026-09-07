package com.networkmonitor.util;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * DateUtil - Utility class for date/time formatting and conversions
 *
 * Used to:
 * - Format timestamps for display in UI
 * - Convert between Date and Timestamp
 * - Parse date strings
 */
public class DateUtil {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    /**
     * Format a Timestamp to date string (yyyy-MM-dd)
     *
     * @param timestamp Timestamp object
     * @return Formatted date string
     */
    public static String formatDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return DATE_FORMAT.format(new Date(timestamp.getTime()));
    }

    /**
     * Format a Timestamp to datetime string (yyyy-MM-dd HH:mm:ss)
     *
     * @param timestamp Timestamp object
     * @return Formatted datetime string
     */
    public static String formatDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return DATETIME_FORMAT.format(new Date(timestamp.getTime()));
    }

    /**
     * Format a Timestamp to time string (HH:mm:ss)
     *
     * @param timestamp Timestamp object
     * @return Formatted time string
     */
    public static String formatTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return TIME_FORMAT.format(new Date(timestamp.getTime()));
    }

    /**
     * Convert java.util.Date to java.sql.Timestamp
     *
     * @param date Date object
     * @return Timestamp object
     */
    public static Timestamp dateToTimestamp(Date date) {
        if (date == null) {
            return null;
        }
        return new Timestamp(date.getTime());
    }

    /**
     * Convert java.sql.Timestamp to java.util.Date
     *
     * @param timestamp Timestamp object
     * @return Date object
     */
    public static Date timestampToDate(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return new Date(timestamp.getTime());
    }

    /**
     * Get current timestamp
     *
     * @return Current Timestamp
     */
    public static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    /**
     * Get timestamp from milliseconds
     *
     * @param millis Milliseconds since epoch
     * @return Timestamp object
     */
    public static Timestamp fromMillis(long millis) {
        return new Timestamp(millis);
    }

    /**
     * Parse a date string (yyyy-MM-dd) to Timestamp
     *
     * @param dateStr Date string in yyyy-MM-dd format
     * @return Timestamp object
     */
    public static Timestamp parseDate(String dateStr) {
        try {
            Date date = DATE_FORMAT.parse(dateStr);
            return new Timestamp(date.getTime());
        } catch (Exception e) {
            System.err.println("[DateUtil] Error parsing date: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get time difference in seconds between two timestamps
     *
     * @param start Start timestamp
     * @param end End timestamp
     * @return Difference in seconds
     */
    public static long getSecondsDifference(Timestamp start, Timestamp end) {
        if (start == null || end == null) {
            return 0;
        }
        return (end.getTime() - start.getTime()) / 1000;
    }

    /**
     * Check if a timestamp is older than N hours
     *
     * @param timestamp Timestamp to check
     * @param hours Number of hours
     * @return true if timestamp is older than N hours
     */
    public static boolean isOlderThanHours(Timestamp timestamp, int hours) {
        if (timestamp == null) {
            return false;
        }
        long hoursInMillis = (long) hours * 60 * 60 * 1000;
        long diff = System.currentTimeMillis() - timestamp.getTime();
        return diff > hoursInMillis;
    }
}
