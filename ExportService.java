package com.networkmonitor.service;

import com.networkmonitor.model.NetworkMetric;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class ExportService {

    public static void exportMetricsToCSV(List<NetworkMetric> metrics, java.awt.Component parent) {
        if (metrics == null || metrics.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No data to export.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export as CSV");
        fileChooser.setSelectedFile(new File("network_metrics_export.csv"));

        int userSelection = fileChooser.showSaveDialog(parent);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (PrintWriter out = new PrintWriter(new FileWriter(fileToSave))) {
                // Header
                out.println("MetricID,DeviceID,Bandwidth(Mbps),Latency(ms),PacketLoss(%),Timestamp");
                
                // Data
                for (NetworkMetric metric : metrics) {
                    out.printf("%d,%d,%.2f,%.2f,%.2f,%s\n",
                        metric.getMetricId(),
                        metric.getDeviceId(),
                        metric.getBandwidthUsage(),
                        metric.getLatencyMs(),
                        metric.getPacketLossPct(),
                        metric.getRecordedAt() != null ? metric.getRecordedAt().toString() : ""
                    );
                }
                
                JOptionPane.showMessageDialog(parent, "Export successful!\n" + fileToSave.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(parent, "Error writing file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
