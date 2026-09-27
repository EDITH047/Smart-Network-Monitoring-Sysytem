# Smart Network Monitoring, Security & Optimization System

## Overview
The **Smart Network Monitoring System** is a robust, Java Swing-based desktop application designed for real-time network telemetry, security management, and optimization. It provides network administrators and operators with a centralized dashboard to track device performance, manage firewall policies, detect security threats, and automatically optimize network traffic.

## Key Features

### 📊 Real-Time Telemetry & Monitoring
* **1-Second Live Performance Feed:** Actively polls bandwidth (`rxBytesPerSec` / `txBytesPerSec`), packet loss, and latency for active network adapters.
* **Dynamic Dashboards:** High-contrast UI featuring live data tables, graphical indicators, and historical summaries.
* **Cross-Platform Network Probes:** Uses native system tools (e.g., `ping`, OS-level adapter statistics) seamlessly managed via `NetworkAdapter`.

### 🛡️ Security & Firewall Management
* **Intrusion & Anomaly Detection:** Identifies abnormal traffic patterns, port scans, or unauthorized access attempts.
* **Firewall Rules:** Add, edit, and enforce network rules (Block/Allow IP, Port Restrictions, Protocol specific filtering).
* **Blocked IP Registry:** Track and permanently ban malicious actors across the network.

### 📱 Device Registry
* **Auto-Discovery:** Automatically detects and registers genuine physical network interfaces on the local machine on startup.
* **Automatic Cleanup:** Safely purges stale, non-physical, or mock devices every time the application launches to ensure a clean dashboard.
* Live status tracking (Online / Offline).

### ⚡ Network Optimization
* **Automated Diagnostics:** Analyzes historical metrics to detect bottlenecks and misconfigurations.
* **Optimization Recommendations:** Suggests actionable improvements (e.g., DNS caching, MTU adjustments, TCP Window Scaling).

### 🔔 Smart Alerting System
* Configurable threshold-based alerts (e.g., latency > 100ms, packet loss > 5%).
* Real-time notification badge on the dashboard header.
* Comprehensive alert history with Acknowledgement workflows.

### 👥 User Roles & Audit Logging
* **Role-Based Access Control (RBAC):** Supports `ADMIN` and `OPERATOR` roles with differentiated permissions.
* **Security Auditing:** Logs all critical actions (logins, rule changes, device deletions) to an un-editable audit log.
* **Secure Authentication:** Passwords securely hashed; login attempts rate-limited.

---

## Technology Stack
* **Language:** Java 21 (Temurin OpenJDK 21.0.11)
* **GUI Framework:** Java Swing (AWT / Swing)
* **Database:** MySQL Community Server 8.4
* **Database Driver:** MySQL Connector/J 8.4.0
* **External Libraries:** JFreeChart 1.5.6 (For Analytics), JCommon 1.0.24

---

## Environment Setup & Installation

### 1. Database Configuration
The application automatically starts the embedded MySQL instance from the `mysql-data` directory on port `3306`. 
*Note: The application performs a clean shutdown on exit, automatically purging all temporary runtime data (devices, metrics, alerts, etc.) while preserving user credentials. Your dashboard will start fresh each time.*

If the database needs to be recreated or manually launched:

```powershell
# To manually start MySQL:
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' --basedir='C:\Program Files\MySQL\MySQL Server 8.4' --datadir="$PWD\mysql-data" --port=3306 --bind-address=127.0.0.1 --console

# To rebuild the schema from scratch:
Get-Content Documents\schema.sql -Raw | & 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' --protocol=tcp -h 127.0.0.1 -P 3306 -u root
```

### 2. Compilation and Execution
To compile and run the application, double-click the `run.bat` file in the root directory. 

Alternatively, run from the command line:
```bat
# Compile
javac -cp "lib/*" -d out *.java

# Launch
java -cp "out;lib/*" com.networkmonitor.main.MainApp
```

### 3. Default Credentials
* **Username:** admin
* **Password:** admin123

## Architecture Overview
* **Model Layer (`com.networkmonitor.model`):** Data Transfer Objects (User, Device, NetworkMetric, Alert).
* **DAO Layer (`com.networkmonitor.dao`):** Pure JDBC database interaction (CRUD operations).
* **Service Layer (`com.networkmonitor.service`):** Business logic, metric aggregation, threading.
* **UI Layer (`com.networkmonitor.ui`):** Component-based Swing Panels (MonitoringPanel, SecurityPanel, etc.).
* **Util Layer (`com.networkmonitor.util`):** Cross-cutting concerns like Themes, Validation, and the `NetworkAdapter` OS bridge.