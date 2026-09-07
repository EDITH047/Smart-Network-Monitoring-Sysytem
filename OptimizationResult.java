package com.networkmonitor.model;

import java.sql.Timestamp;

/**
 * OptimizationResult - POJO representing bandwidth optimization analysis results
 * Maps to: optimization_results table
 */
public class OptimizationResult {
    private int resultId;
    private int deviceId;
    private double currentBandwidth;
    private double recommendedBandwidth;
    private int optimizationScore; // 0-100
    private String suggestion;
    private Timestamp analyzedAt;

    // Constructors
    public OptimizationResult() {
    }

    public OptimizationResult(int deviceId, double currentBandwidth, double recommendedBandwidth,
                             int optimizationScore, String suggestion) {
        this.deviceId = deviceId;
        this.currentBandwidth = currentBandwidth;
        this.recommendedBandwidth = recommendedBandwidth;
        this.optimizationScore = optimizationScore;
        this.suggestion = suggestion;
    }

    public OptimizationResult(int resultId, int deviceId, double currentBandwidth,
                             double recommendedBandwidth, int optimizationScore,
                             String suggestion, Timestamp analyzedAt) {
        this.resultId = resultId;
        this.deviceId = deviceId;
        this.currentBandwidth = currentBandwidth;
        this.recommendedBandwidth = recommendedBandwidth;
        this.optimizationScore = optimizationScore;
        this.suggestion = suggestion;
        this.analyzedAt = analyzedAt;
    }

    // Getters and Setters
    public int getResultId() {
        return resultId;
    }

    public void setResultId(int resultId) {
        this.resultId = resultId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public double getCurrentBandwidth() {
        return currentBandwidth;
    }

    public void setCurrentBandwidth(double currentBandwidth) {
        this.currentBandwidth = currentBandwidth;
    }

    public double getRecommendedBandwidth() {
        return recommendedBandwidth;
    }

    public void setRecommendedBandwidth(double recommendedBandwidth) {
        this.recommendedBandwidth = recommendedBandwidth;
    }

    public int getOptimizationScore() {
        return optimizationScore;
    }

    public void setOptimizationScore(int optimizationScore) {
        this.optimizationScore = optimizationScore;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public Timestamp getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(Timestamp analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    @Override
    public String toString() {
        return "OptimizationResult{" +
                "resultId=" + resultId +
                ", deviceId=" + deviceId +
                ", currentBandwidth=" + currentBandwidth +
                ", recommendedBandwidth=" + recommendedBandwidth +
                ", optimizationScore=" + optimizationScore +
                ", suggestion='" + suggestion + '\'' +
                ", analyzedAt=" + analyzedAt +
                '}';
    }
}
