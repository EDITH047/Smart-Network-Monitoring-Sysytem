package com.networkmonitor.service;

import com.networkmonitor.dao.NetworkMetricDAO;
import com.networkmonitor.dao.OptimizationDAO;
import com.networkmonitor.model.NetworkMetric;
import com.networkmonitor.model.OptimizationResult;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;

/**
 * OptimizationService - Handles bandwidth optimization analysis and recommendations
 * Uses: OptimizationDAO, NetworkMetricDAO
 */
public class OptimizationService {

    private static final OptimizationService instance = new OptimizationService();
    private OptimizationDAO optimizationDAO = new OptimizationDAO();
    private NetworkMetricDAO metricDAO = new NetworkMetricDAO();

    private OptimizationService() {
    }

    public static OptimizationService getInstance() {
        return instance;
    }

    /**
     * Analyze ALL devices using a single aggregated DB query (eliminates N+1).
     * Does NOT affect MonitoringService's live collect/persist loops.
     */
    public List<OptimizationResult> analyzeAll() {
        System.out.println("[OptimizationService] Starting efficient bulk network analysis...");

        // ONE read query for all devices (uses idx_metrics_time index)
        List<NetworkMetricDAO.AggregatedMetrics> allMetrics =
                metricDAO.getAggregatedMetricsForAllDevices(24);

        List<OptimizationResult> resultsToSave = new ArrayList<>();

        for (NetworkMetricDAO.AggregatedMetrics metrics : allMetrics) {
            int score = calculateOptimizationScore(metrics.avgBandwidth, metrics.peakBandwidth);
            String suggestion = generateSuggestion(metrics.avgBandwidth, metrics.peakBandwidth, score);
            double recommendedBandwidth = metrics.peakBandwidth * 1.2;

            OptimizationResult result = new OptimizationResult(
                    metrics.deviceId,
                    metrics.avgBandwidth,
                    recommendedBandwidth,
                    score,
                    suggestion
            );
            resultsToSave.add(result);
            System.out.println("[OptimizationService] Device " + metrics.deviceId + " scored: " + score);
        }

        // ONE batch write for all results (transactional, uses idx_opt_device_time)
        if (!resultsToSave.isEmpty()) {
            optimizationDAO.batchInsertResults(resultsToSave);
        }

        System.out.println("[OptimizationService] Bulk analysis complete. Analyzed " + resultsToSave.size() + " devices.");
        return optimizationDAO.getLatestResults();
    }

    /**
     * Analyze a specific device. Kept for individual-device use cases.
     * Uses existing indexed per-device queries (device_id is part of idx_metrics_device_time).
     */
    public OptimizationResult analyzeDevice(int deviceId) {
        NetworkMetric avgMetric = metricDAO.getAverageMetrics(deviceId, 24);

        if (avgMetric == null) {
            System.out.println("[OptimizationService] No metrics available for device: " + deviceId);
            return null;
        }

        double peakBandwidth = metricDAO.getPeakBandwidth(deviceId, 24);
        int score = calculateOptimizationScore(avgMetric.getBandwidthUsage(), peakBandwidth);
        String suggestion = generateSuggestion(avgMetric.getBandwidthUsage(), peakBandwidth, score);
        double recommendedBandwidth = peakBandwidth * 1.2;

        OptimizationResult result = new OptimizationResult(
                deviceId,
                avgMetric.getBandwidthUsage(),
                recommendedBandwidth,
                score,
                suggestion
        );

        optimizationDAO.insertResult(result); // Single insert — correct for one device
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
