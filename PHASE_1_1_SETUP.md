# Phase 1.1: Database Configuration & Connection Test
## Setup Instructions

### Step 1: Create Project Structure

In your IDE (IntelliJ, Eclipse, or NetBeans), create the following package structure:

```
src/
├── com.networkmonitor.config/
├── com.networkmonitor.model/
├── com.networkmonitor.dao/
├── com.networkmonitor.service/
├── com.networkmonitor.ui/
├── com.networkmonitor.util/
├── com.networkmonitor.main/
└── com.networkmonitor.test/
```

### Step 2: Add MySQL JDBC Driver

1. Download MySQL Connector/J from: https://dev.mysql.com/downloads/connector/j/
   - Or use version 8.0.33 (latest stable)

2. Add the JAR file to your project:
   - **IntelliJ**: Project Structure → Libraries → + → Select JAR → `mysql-connector-java-8.x.jar`
   - **Eclipse**: Project → Properties → Java Build Path → Libraries → Add External JAR
   - **NetBeans**: Right-click Project → Properties → Libraries → Add JAR/Folder

### Step 3: Set Up MySQL Database

1. Open MySQL Command Line or MySQL Workbench
2. Run the schema.sql file:
   ```bash
   mysql -u root -p < schema.sql
   ```
   Or paste the contents in MySQL Workbench and execute

3. Verify the database was created:
   ```sql
   USE network_monitor_db;
   SHOW TABLES;
   ```
   You should see 9 tables: users, devices, network_metrics, security_events, firewall_rules, alerts, optimization_results, blocked_ips, audit_log

### Step 4: Create Java Classes

Copy the following files to your project:

1. **DatabaseConfig.java** → `src/com/networkmonitor/config/`
2. **ConnectionTest.java** → `src/com/networkmonitor/test/`

### Step 5: Run Connection Test

1. In your IDE, navigate to `ConnectionTest.java`
2. Right-click → Run As → Java Application (or press Ctrl+F11 / Cmd+F11)
3. Check the Console output

### Expected Output (Success)

```
========================================
Smart Network Monitoring System
Connection Test
========================================

[1] Testing database connection...
✓ Connection successful!
   Database: network_monitor_db
   URL: jdbc:mysql://localhost:3306/network_monitor_db

[2] Verifying database tables...
   ✓ users
   ✓ devices
   ✓ network_metrics
   ✓ security_events
   ✓ firewall_rules
   ✓ alerts
   ✓ optimization_results
   ✓ blocked_ips
   ✓ audit_log

[3] Checking sample data...
   Users in database: 1
   Devices in database: 5
   ✓ Admin user exists

[4] Admin user details:
   User ID: 1
   Username: admin
   Email: admin@network.local
   Role: ADMIN
   Password hint: admin123 (hashed in DB)

[5] Sample devices:
   [1] Core Router | IP: 192.168.1.1 | Type: ROUTER | Status: ONLINE
   [2] Main Switch | IP: 192.168.1.2 | Type: SWITCH | Status: ONLINE
   [3] Web Server | IP: 192.168.1.10 | Type: SERVER | Status: ONLINE
   [4] Wi-Fi AP Floor 1 | IP: 192.168.1.20 | Type: ACCESS_POINT | Status: WARNING
   [5] Edge Firewall | IP: 192.168.1.254 | Type: FIREWALL | Status: ONLINE

========================================
✓ All tests passed!
✓ Database is ready for the application
========================================
```

### Troubleshooting

#### Error: "MySQL JDBC Driver not found"
- **Fix**: MySQL Connector/J JAR is not in the classpath
- Add it to your project libraries (see Step 2)

#### Error: "Connection refused (127.0.0.1:3306)"
- **Fix**: MySQL server is not running
- Start MySQL: `mysql.server start` (Mac/Linux) or start the MySQL service (Windows)

#### Error: "Access denied for user 'root'@'localhost'"
- **Fix**: Wrong password or username
- Update credentials in `DatabaseConfig.java` lines 17-18:
  ```java
  private static final String DB_USER = "your_username";
  private static final String DB_PASSWORD = "your_password";
  ```

#### Error: "Unknown database 'network_monitor_db'"
- **Fix**: Schema hasn't been executed
- Run schema.sql in MySQL (see Step 3)

---

## What's Next?

Once the connection test passes, we'll move to Phase 1.2: Create all Model (POJO) classes.

These are simple data classes that map each database table to a Java object. They'll be used by the DAO layer.

