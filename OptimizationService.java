package com.networkmonitor.service;

import com.networkmonitor.dao.NetworkMetricDAO;
import com.networkmonitor.dao.OptimizationDAO;
import com.networkmonitor.model.NetworkMetric;
import com.networkmonitor.model.OptimizationResult;
import java.sql.Timestamp;
import java.util.List;

/**
 * OptimizationService - Handles bandwidth optimization analysis and recommendations
 * Uses: OptimizationDAO, NetworkMetricDAO
 */
public class OptimizationService {

    private static OptimizationService instance;
    private OptimizationDAO optimizationDAO = new OptimizationDAO();
    private NetworkMetricDAO metricDAO = new NetworkMetricDAO();

    private OptimizationService() {
    }

    public static OptimizationService getInstance() {
        if (instance == null) {
            instance = new OptimizationService();
        }
        return instance;
    }

    /**
     * Analyze all devices and generate optimization results
     */
    public List<OptimizationResult> analyzeAll() {
        System.out.println("[OptimizationService] Starting network analysis...");

        MonitoringService monitoringService = MonitoringService.getInstance();
        List<com.networkmonitor.model.Device> devices =
            new com.networkmonitor.dao.DeviceDAO().getAllDevices();

        for (com.networkmonitor.model.Device device : devices) {
            try {
                analyzeDevice(device.getDeviceId());
            } catch (Exception e) {
                System.err.println("[OptimizationService] Error analyzing device " + device.getDeviceId() + ": " + e.getMessage());
            }
        }

        System.out.println("[OptimizationService] Analysis complete");
        return optimizationDAO.getLatestResults();
    }

    /**
     * Analyze a specific device
     */
    public OptimizationResult analyzeDevice(int deviceId) {
        // Get average metrics for last 24 hours
        NetworkMetric avgMetric = metricDAO.getAverageMetrics(deviceId, 24);

        if (avgMetric == null) {
            System.out.println("[OptimizationService] No metrics available for device: " + deviceId);
            return null;
        }

        // Get peak bandwidth
        double peakBandwidth = metricDAO.getPeakBandwidth(deviceId, 24);

        // Calculate optimization score (0-100)
        int score = calculateOptimizationScore(avgMetric.getBandwidthUsage(), peakBandwidth);

        // Generate recommendation
        String suggestion = generateSuggestion(avgMetric.getBandwidthUsage(), peakBandwidth, score);

        // Calculate recommended bandwidth (with 20% headroom)
        double recommendedBandwidth = peakBandwidth * 1.2;

        // Create result
        OptimizationResult result = new OptimizationResult(
                deviceId,
                avgMetric.getBandwidthUsage(),
                recommendedBandwidth,
                score,
                suggestion
        );

        // Save to database
        optimizationDAO.insertResult(result);

        System.out.println("[OptimizationService] Device " + deviceId + " analyzed. Score: " + score);

        return result;
    }

    /**
     * Calculate optimization score (0-100)
     * Higher score = better optimization
     */
    private int calculateOptimizationScore(double avgBandwidth, double peakBandwidth) {
        if (peakBandwidth == 0) {
            return 100;
        }

        // Utilization ratio (0-1)
        double utilizationRatio = avgBandwidth / peakBandwidth;

        // Ideal utilization is 60-80%
        int score;

        if (utilizationRatio < 0.5) {
            // Under-utilized: wasting capacity
            score = (int) (utilizationRatio * 100);
        } else if (utilizationRatio >= 0.5 && utilizationRatio <= 0.8) {
            // Optimal range
            score = (int) (75 + (utilizationRatio - 0.5) * 50);
        } else {
            // Over-utilized: potential bottleneck
            score = Math.max(50, (int) (100 - (utilizationRatio - 0.8) * 200));
        }

        return Math.min(100, Math.max(0, score));
    }

    /**
     * Generate optimization suggestion
     */
    private String generateSuggestion(double avgBandwidth, double peakBandwidth, int score) {
        if (score >= 80) {
            return "Device is well-optimized. Current configuration is efficient.";
        } else if (score >= 60) {
            return "Good optimization. Minor improvements possible by reducing peak bandwidth fluctuations.";
        } else if (score >= 40) {
            return "Bandwidth utilization could be improved. Consider load balancing or traffic management.";
        } else if (avgBandwidth < peakBandwidth * 0.5) {
            return "Device is under-utilized. Consider consolidating traffic or reducing allocated bandwidth.";
        } else {
            return "Device approaching capacity. Increase available bandwidth or implement QoS policies.";
        }
    }

    /**
     * Get latest results for all devices
     */
    public List<OptimizationResult> getLatestResults() {
        return optimizationDAO.getLatestResults();
    }

    /**
     * Get latest result for a specific device
     */
    public OptimizationResult getLatestResultByDevice(int deviceId) {
        return optimizationDAO.getLatestResultByDevice(deviceId);
    }

    /**
     * Get devices with low optimization score
     */
    public List<OptimizationResult> getLowScoringDevices(int threshold) {
        return optimizationDAO.getLowScoreResults(threshold);
    }

    /**
     * Get devices with high optimization score
     */
    public List<OptimizationResult> getHighScoringDevices(int threshold) {
        return optimizationDAO.getHighScoreResults(threshold);
    }

    /**
     * Get average optimization score across network
     */
    public double getNetworkAverageScore() {
        return optimizationDAO.getAverageScore();
    }
}
