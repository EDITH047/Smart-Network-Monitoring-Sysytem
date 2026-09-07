package com.networkmonitor.util;

import javax.swing.JFileChooser;
import javax.swing.JTable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * CSVExporter - Utility class to export JTable data to CSV files
 *
 * Used to:
 * - Export monitoring data to CSV
 * - Export security events to CSV
 * - Export reports to CSV
 * - Generate downloadable reports
 */
public class CSVExporter {

    /**
     * Export JTable data to CSV file
     *
     * @param table JTable to export
     * @param fileName Default filename for the save dialog
     * @return true if export successful, false otherwise
     */
    public static boolean exportTableToCSV(JTable table, String fileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(fileName + ".csv"));

        int result = fileChooser.showSaveDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                writeTableToFile(table, file);
                System.out.println("[CSVExporter] Successfully exported to: " + file.getAbsolutePath());
                return true;
            } catch (IOException e) {
                System.err.println("[CSVExporter] Error exporting file: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * Export JTable data to CSV file (with specified path)
     *
     * @param table JTable to export
     * @param filePath Full file path including filename
     * @return true if export successful, false otherwise
     */
    public static boolean exportTableToCSV(JTable table, String filePath, boolean showDialog) {
        if (showDialog) {
            return exportTableToCSV(table, filePath);
        }

        try {
            writeTableToFile(table, new File(filePath));
            System.out.println("[CSVExporter] Successfully exported to: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("[CSVExporter] Error exporting file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Write JTable data to CSV file
     *
     * @param table JTable to export
     * @param file Output file
     * @throws IOException if write fails
     */
    private static void writeTableToFile(JTable table, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {

            // Write header
            int columnCount = table.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                writer.write(escapeCSV(table.getColumnName(i)));
                if (i < columnCount - 1) {
                    writer.write(",");
                }
            }
            writer.write("\n");

            // Write data rows
            int rowCount = table.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                for (int j = 0; j < columnCount; j++) {
                    Object value = table.getValueAt(i, j);
                    String cellValue = value != null ? value.toString() : "";
                    writer.write(escapeCSV(cellValue));

                    if (j < columnCount - 1) {
                        writer.write(",");
                    }
                }
                writer.write("\n");
            }

            writer.flush();
        }
    }

    /**
     * Escape special characters in CSV values
     *
     * @param value Value to escape
     * @return Escaped value
     */
    private static String escapeCSV(String value) {
        if (value == null) {
            return "";
        }

        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    /**
     * Export data array to CSV file
     *
     * @param headers Column headers
     * @param data 2D array of data (rows x columns)
     * @param fileName Default filename
     * @return true if export successful, false otherwise
     */
    public static boolean exportDataToCSV(String[] headers, String[][] data, String fileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(fileName + ".csv"));

        int result = fileChooser.showSaveDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                writeDataToFile(headers, data, file);
                System.out.println("[CSVExporter] Successfully exported to: " + file.getAbsolutePath());
                return true;
            } catch (IOException e) {
                System.err.println("[CSVExporter] Error exporting file: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * Write data array to CSV file
     *
     * @param headers Column headers
     * @param data 2D array of data
     * @param file Output file
     * @throws IOException if write fails
     */
    private static void writeDataToFile(String[] headers, String[][] data, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {

            // Write headers
            for (int i = 0; i < headers.length; i++) {
                writer.write(escapeCSV(headers[i]));
                if (i < headers.length - 1) {
                    writer.write(",");
                }
            }
            writer.write("\n");

            // Write data
            if (data != null) {
                for (String[] row : data) {
                    for (int i = 0; i < row.length; i++) {
                        writer.write(escapeCSV(row[i]));
                        if (i < row.length - 1) {
                            writer.write(",");
                        }
                    }
                    writer.write("\n");
                }
            }

            writer.flush();
        }
    }

    /**
     * Test the CSV exporter (creates a sample CSV file)
     */
    public static void main(String[] args) {
        String[] headers = {"Device Name", "IP Address", "Status", "Bandwidth"};
        String[][] data = {
                {"Core Router", "192.168.1.1", "ONLINE", "45.5 Mbps"},
                {"Main Switch", "192.168.1.2", "ONLINE", "62.3 Mbps"},
                {"Web Server", "192.168.1.10", "ONLINE", "89.1 Mbps"}
        };

        exportDataToCSV(headers, data, "test_export");
        System.out.println("CSV export test completed!");
    }
}
