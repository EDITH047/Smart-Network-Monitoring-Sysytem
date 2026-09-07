# Phase 2.1 Testing Guide - Login UI & Dashboard

## Pre-Testing Checklist

### Step 1: Verify Project Structure

Ensure all files are in correct packages:

```
src/com/networkmonitor/
├── config/
│   └── DatabaseConfig.java ✓
├── model/ (9 files) ✓
├── util/ (4 files) ✓
├── dao/ (9 files) ✓
├── service/ (7 files) ✓
├── ui/
│   ├── LoginFrame.java ✓ NEW
│   ├── MainDashboard.java ✓ NEW
│   └── UIPanels.java ✓ NEW (contains 8 panel classes)
├── main/
│   └── MainApp.java ✓ NEW
└── test/
    └── ConnectionTest.java ✓
```

### Step 2: Verify MySQL Database

1. Ensure MySQL server is running
2. Database `network_monitor_db` exists
3. All 9 tables created (from schema.sql)
4. Sample data populated:
   - At least 1 admin user (admin/admin123)
   - At least 5 sample devices

**Quick verification:**
```bash
mysql -u root -p network_monitor_db
mysql> SELECT COUNT(*) FROM users;  # Should show 1+
mysql> SELECT COUNT(*) FROM devices;  # Should show 5+
```

### Step 3: Verify MySQL Connector JAR

- `mysql-connector-java-8.x.jar` is in classpath
- IDE recognizes it (no red squiggles on imports)

---

## Compilation Testing

### Test 1: Compile All Files

**In your IDE:**
1. Right-click project → Build (or Ctrl+B / Cmd+B)
2. Verify: **No compilation errors**

**Expected result:**
```
BUILD SUCCESSFUL
✓ 0 errors, 0 warnings
```

**If you get errors:**
- Check package names match directory structure
- Verify import statements are correct
- Ensure all dependencies are in classpath

---

## Unit Testing: Individual Components

### Test 2: Test DatabaseConfig Connection

**Run:** `ConnectionTest.main()`

**Expected output:**
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
   ...

========================================
✓ All tests passed!
✓ Database is ready for the application
========================================
```

**If this fails:**
- MySQL server not running
- Wrong credentials in DatabaseConfig
- Database not created from schema.sql
- MySQL JDBC driver not in classpath

---

## Integration Testing: UI Components

### Test 3: LoginFrame Functionality

**Run:** `LoginFrame.main()`

**What you should see:**
```
✓ Window opens with title: "Smart Network Monitoring System - Login"
✓ Window is centered on screen
✓ Window size is 500x400 (not resizable)
```

**UI Elements Check:**
```
☐ Title: "Smart Network Monitoring System" (blue, bold)
☐ Subtitle: "Secure Login" (gray)
☐ Username label and text field
☐ Password label and password field
☐ Login button (blue background)
☐ Exit button (gray background)
☐ Error label (red, initially empty)
☐ Demo credentials info at bottom
```

**Interaction Tests:**

#### Test 3a: Invalid Login
1. Leave username empty
2. Click Login
3. **Expected:** Error: "Please enter username and password"

#### Test 3b: Wrong Credentials
1. Username: `admin`
2. Password: `wrongpassword`
3. Click Login
4. **Expected:** Error: "Invalid username or password"
5. Password field should be cleared

#### Test 3c: Successful Login
1. Username: `admin`
2. Password: `admin123`
3. Click Login
4. **Expected:**
   - Console shows: "[LoginFrame] Login successful for: admin"
   - LoginFrame closes
   - MainDashboard opens

#### Test 3d: Enter Key Submission
1. Username: `admin`
2. Password: `admin123`
3. Press Enter in password field (instead of clicking button)
4. **Expected:** Same as Test 3c - should login

#### Test 3e: Exit Button
1. Click Exit button
2. **Expected:** Application closes

---

### Test 4: MainDashboard Functionality

**Prerequisites:** Successfully logged in as admin

**Window Check:**
```
✓ Window title shows: "Smart Network Monitoring System - admin (ADMIN)"
✓ Window is maximized or near-full screen
✓ Window has header, tabbed area, and footer
```

**Header Panel Check:**
```
☐ Left: "Smart Network Monitoring System" title
☐ Center: "🔔 Alerts: 0" (or updated count)
☐ Right: "👤 admin (ADMIN)" user info
☐ Right: Red "Logout" button
```

**Tabs Check (For ADMIN user - should see ALL 8 tabs):**
```
☐ 📊 Monitoring (tab 1)
☐ 🔔 Alerts (tab 2)
☐ 📈 Reports (tab 3)
☐ 📱 Devices (tab 4)
☐ 🛡️ Security (tab 5)
☐ 🔥 Firewall (tab 6)
☐ ⚡ Optimization (tab 7)
☐ 👥 Users (tab 8)
```

**Footer Panel Check:**
```
☐ Left: "Status: Connected ✓" (green checkmark)
☐ Right: "Last updated: [current date/time]"
```

**Interaction Tests:**

#### Test 4a: Tab Switching
1. Click each tab
2. **Expected:** Tab content switches smoothly
3. Each tab shows placeholder text: "[Icon] [Name] Panel - Coming Soon"

#### Test 4b: Alert Badge Update
1. Wait 10+ seconds (alert refresh interval)
2. **Expected:** Alert badge updates (should say "🔔 Alerts: [count]")
3. If no new alerts: "🔔 Alerts: None"

#### Test 4c: Logout Button
1. Click Logout button
2. **Expected:**
   - Console shows: "[MainDashboard] User logged out"
   - MainDashboard closes
   - LoginFrame reappears
   - Can log in again

#### Test 4d: Window Close Button
1. Click window close (X button)
2. **Expected:** Prompts logout, returns to LoginFrame

---

### Test 5: Role-Based Tab Visibility

**Test 5a: Login as VIEWER (create test user if needed)**
```sql
INSERT INTO users (username, password_hash, full_name, email, role, is_active)
VALUES ('viewer', 
        '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
        'Test Viewer', 'viewer@test.com', 'VIEWER', TRUE);
```

1. Login with viewer/admin123
2. **Expected:** Only 3 tabs visible:
   - 📊 Monitoring
   - 🔔 Alerts
   - 📈 Reports
3. Device/Security/Firewall/Optimization/Users tabs hidden

**Test 5b: Login as OPERATOR (create test user if needed)**
```sql
INSERT INTO users (username, password_hash, full_name, email, role, is_active)
VALUES ('operator',
        '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
        'Test Operator', 'operator@test.com', 'OPERATOR', TRUE);
```

1. Login with operator/admin123
2. **Expected:** 7 tabs visible:
   - 📊 Monitoring
   - 🔔 Alerts
   - 📈 Reports
   - 📱 Devices
   - 🛡️ Security
   - 🔥 Firewall
   - ⚡ Optimization
3. Users tab hidden (ADMIN only)

**Test 5c: Login as ADMIN**
1. Login with admin/admin123
2. **Expected:** All 8 tabs visible including 👥 Users

---

## Console Output Verification

When running the application, check console output:

**Expected startup messages:**
```
================================================================================
Smart Network Monitoring, Security & Optimization System
Version 1.0
================================================================================

📋 Default Credentials:
   Username: admin
   Password: admin123

🚀 Launching Login Frame...

```

**Expected login messages (on successful login):**
```
[DatabaseConfig] New connection established
[UserDAO] User found: admin
[AuthService] Login attempt: admin from 127.0.0.1
[AuthService] Login successful: admin (Role: ADMIN)
[LoginFrame] Login successful for: admin
```

**Expected logout messages:**
```
[AuthService] User logged out: admin
[MainDashboard] User logged out
```

---

## Error Handling Tests

### Test 6a: Database Connection Failure
1. Stop MySQL server
2. Run application
3. **Expected:** Error during login, clear error message
4. **Console shows:** "[DatabaseConfig] Connection failed..."

### Test 6b: Invalid Database
1. Change database name in DatabaseConfig to invalid name
2. Try to login
3. **Expected:** Connection error, graceful handling

### Test 6c: Missing Tables
1. Drop a table from database
2. Try to login
3. **Expected:** Error logged, application handles gracefully

---

## Performance Testing

### Test 7: Alert Badge Refresh Performance
1. Keep application open for 5 minutes
2. Monitor alert badge updates (should happen every 10 seconds)
3. **Expected:** No memory leaks, consistent updates
4. Check task manager - memory usage stable

### Test 8: Tab Switching Speed
1. Rapidly switch between tabs
2. **Expected:** Smooth, no lag or freezing
3. All tabs respond quickly

---

## Final Verification Checklist

Run through this before declaring Phase 2.1 complete:

```
COMPILATION
☐ All 35 files compile without errors
☐ No warnings in build output
☐ IDE shows no red squiggles on imports

DATABASE
☐ MySQL running and connected
☐ Database tables all exist
☐ Admin user (admin/admin123) exists
☐ Sample devices exist

LOGIN FRAME
☐ Displays correctly centered
☐ Username/password fields work
☐ Enter key submits login
☐ Invalid login shows error
☐ Valid login opens dashboard
☐ Exit button works

MAIN DASHBOARD
☐ Opens with correct title showing user role
☐ All expected tabs visible for user role
☐ Tabs are clickable and switch content
☐ Alert badge displays and updates
☐ Header shows user info and logout button
☐ Footer shows status and timestamp
☐ Logout button works correctly
☐ Window close triggers logout

ROLE-BASED ACCESS
☐ ADMIN sees all 8 tabs
☐ OPERATOR sees 7 tabs (no Users)
☐ VIEWER sees 3 tabs only

CONSOLE OUTPUT
☐ No error messages
☐ Startup messages appear
☐ Login/logout events logged
☐ Database connection confirmed
```

---

## Troubleshooting Guide

### Problem: "Cannot find symbol: class LoginFrame"
**Solution:** Check package name in LoginFrame.java matches `com.networkmonitor.ui`

### Problem: "class LoginFrame is public, should be declared in a file named LoginFrame.java"
**Solution:** File name must match class name exactly

### Problem: "Connection refused" on login
**Solution:** 
- Start MySQL server: `mysql.server start` (Mac/Linux) or start MySQL service (Windows)
- Check credentials in DatabaseConfig match your MySQL setup

### Problem: "Unknown database 'network_monitor_db'"
**Solution:** Run schema.sql to create database and tables

### Problem: UI elements overlap or look wrong
**Solution:** Check screen resolution, may need to adjust window size or DPI settings

### Problem: Alert badge doesn't update
**Solution:** 
- Check timer is running (should update every 10 seconds)
- Verify AlertService is accessible from MainDashboard
- Check console for errors

### Problem: Logout button not visible
**Solution:** Check header panel layout, may be off-screen if window too narrow

---

## Success Criteria

Phase 2.1 Testing is **COMPLETE** when:

✅ **Compilation:** All files compile with 0 errors
✅ **Database:** ConnectionTest passes all checks
✅ **Login UI:** Can log in successfully with admin/admin123
✅ **Dashboard:** Opens correctly with user role displayed
✅ **Tabs:** Correct tabs visible based on user role
✅ **Functionality:** All buttons work (logout, tab switching)
✅ **No Crashes:** Application runs smoothly without exceptions
✅ **Console:** Clean output, no error messages

---

## Next Steps After Testing

Once all tests pass:
1. ✅ Phase 2.1 is verified complete
2. 📝 Document any bugs found
3. 🚀 Ready to move to Phase 2.2 (Device Management)

Good luck with testing! 🧪
