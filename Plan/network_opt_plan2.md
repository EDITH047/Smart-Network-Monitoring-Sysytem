## Goal Description
The user requested that we make the "Network Optimization" feature more efficient and fully functional. 

Currently, the `OptimizationService.analyzeAll()` method retrieves the active devices and then makes two separate database queries (`getAverageMetrics` and `getPeakBandwidth`) for **each device**. It then inserts the calculated `OptimizationResult` back into the database with one query **per device**. 

If a network has 100 devices, this causes **300 sequential database queries**, which is an classic "N+1" performance bottleneck. 

To make it efficient, we will:
1. Introduce a single aggregated SQL query to calculate average and peak bandwidth for **all devices simultaneously** using a `GROUP BY` clause.
2. Introduce a `executeBatch` JDBC insert to save all optimization results in a single transaction.
3. This reduces the number of database queries from `3 * N` down to `2` (one to read, one to write), making the analysis nearly instant regardless of network size.

## User Review Required
> [!IMPORTANT] 
> **Monitoring Page Safety**: You asked to guarantee this won't slow down the live Monitoring Page. The new query filters metrics by `recorded_at`. I reviewed your `schema.sql` and found that the existing index `idx_metrics_device_time` is on `(device_id, recorded_at)`. Because `device_id` is the first column, MySQL cannot efficiently use this index when filtering *only* by `recorded_at`. This would cause a full-table scan that could lock the table and cause stuttering on the live monitoring page!
> 
> **The Fix**: I have updated this plan to include adding a new database index specifically on `(recorded_at)` to guarantee the new query executes in milliseconds without locking the table.

## Open Questions
None.

## Proposed Changes

---

### Database Schema
Add a new index to ensure the new query does not perform a full-table scan, protecting the live Monitoring Service.
#### [MODIFY] `Documents/schema.sql` (and execute live on DB)
```sql
CREATE INDEX idx_metrics_time ON network_metrics(recorded_at);
```

---

### NetworkMetricDAO
Add a new nested Data Transfer Object (DTO) and an aggregated query method to fetch the metrics for all devices at once.
#### [MODIFY] `NetworkMetricDAO.java`
```java
    // Add inside NetworkMetricDAO
    public static class AggregatedMetrics {
        public int deviceId;
        public double avgBandwidth;
        public double peakBandwidth;
    }

    /**
     * Get aggregated average and peak metrics for all devices over the last N hours
     */
    public List<AggregatedMetrics> getAggregatedMetricsForAllDevices(int hours) {
        List<AggregatedMetrics> list = new ArrayList<>();
        String sql = "SELECT device_id, " +
                "AVG(bandwidth_usage) as avg_bw, " +
                "MAX(bandwidth_usage) as peak_bw " +
                "FROM network_metrics " +
                "WHERE recorded_at >= DATE_SUB(NOW(), INTERVAL ? HOUR) " +
                "GROUP BY device_id";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, hours);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                AggregatedMetrics am = new AggregatedMetrics();
                am.deviceId = rs.getInt("device_id");
                am.avgBandwidth = rs.getDouble("avg_bw");
                am.peakBandwidth = rs.getDouble("peak_bw");
                list.add(am);
            }

        } catch (SQLException e) {
            System.err.println("[NetworkMetricDAO] Error getting aggregated metrics: " + e.getMessage());
        }
        return list;
    }
```

---

### OptimizationDAO
Add a batch insert method to write the optimization results in a single high-speed transaction.
#### [MODIFY] `OptimizationDAO.java`
```java
    /**
     * Batch insert optimization results for better performance
     */
    public boolean batchInsertResults(List<OptimizationResult> results) {
        if (results == null || results.isEmpty()) return false;
        
        String sql = "INSERT INTO optimization_results (device_id, current_bandwidth, recommended_bandwidth, optimization_score, suggestion, analyzed_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            // Disable auto-commit for transaction performance
            conn.setAutoCommit(false);

            for (OptimizationResult result : results) {
                ps.setInt(1, result.getDeviceId());
                ps.setDouble(2, result.getCurrentBandwidth());
                ps.setDouble(3, result.getRecommendedBandwidth());
                ps.setInt(4, result.getOptimizationScore());
                ps.setString(5, result.getSuggestion());
                ps.addBatch();
            }

            int[] rows = ps.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
            
            System.out.println("[OptimizationDAO] Batch inserted " + rows.length + " results.");
            return true;

        } catch (SQLException e) {
            System.err.println("[OptimizationDAO] Error batch inserting results: " + e.getMessage());
            return false;
        }
    }
```

---

### OptimizationService
Refactor `analyzeAll()` to use the new efficient DAO methods instead of querying per device.
#### [MODIFY] `OptimizationService.java`
```java
    /**
     * Analyze all devices and generate optimization results
     */
    public List<OptimizationResult> analyzeAll() {
        System.out.println("[OptimizationService] Starting efficient network analysis...");

        List<NetworkMetricDAO.AggregatedMetrics> allMetrics = metricDAO.getAggregatedMetricsForAllDevices(24);
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
            System.out.println("[OptimizationService] Device " + metrics.deviceId + " analyzed. Score: " + score);
        }

        if (!resultsToSave.isEmpty()) {
            optimizationDAO.batchInsertResults(resultsToSave);
        }

        System.out.println("[OptimizationService] Analysis complete");
        return optimizationDAO.getLatestResults();
    }
```

## Verification Plan
### Automated Tests
1. I will execute the `CREATE INDEX` SQL command on the live MySQL database.
2. I will re-compile the project via `javac -cp "lib/*" -d out *.java` to ensure there are no syntax errors introduced by the refactoring.

### Manual Verification
The user can launch the app, navigate to the **Optimization Panel**, and click the "Run Full Analysis" button. 
If the fix is successful, the analysis will complete noticeably faster, and the live monitoring will continue to save metrics seamlessly in the background.
