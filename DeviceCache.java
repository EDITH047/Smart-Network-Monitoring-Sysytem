package com.networkmonitor.service;

import com.networkmonitor.dao.DeviceDAO;
import com.networkmonitor.model.Device;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DeviceCache — Thread-safe in-memory cache for the devices table.
 * Loaded once on startup and invalidated only on add/edit/delete operations.
 * Eliminates all device DB reads from the monitoring hot path.
 *
 * Includes dirty-tracking: only devices whose status actually CHANGED
 * are flushed to the database on the persistence loop.
 */
public class DeviceCache {
    private static final DeviceCache INSTANCE = new DeviceCache();
    
    private final DeviceDAO deviceDAO = new DeviceDAO();
    private final ConcurrentHashMap<Integer, Device> cache = new ConcurrentHashMap<>();
    // Tracks device IDs whose status changed since last DB flush
    private final java.util.Set<Integer> dirtyDeviceIds =
        java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private volatile boolean loaded = false;

    private DeviceCache() {}

    public static DeviceCache getInstance() {
        return INSTANCE;
    }

    /** Load all devices from DB into memory. Called once on startup. */
    public synchronized void reload() {
        cache.clear();
        dirtyDeviceIds.clear();
        List<Device> devices = deviceDAO.getAllDevices();
        for (Device d : devices) {
            cache.put(d.getDeviceId(), d);
        }
        loaded = true;
        System.out.println("[DeviceCache] Loaded " + cache.size() + " devices into cache.");
    }

    /** Ensure the cache is loaded (lazy init). */
    private void ensureLoaded() {
        if (!loaded) reload();
    }

    public List<Device> getAllDevices() {
        ensureLoaded();
        List<Device> list = new ArrayList<>(cache.values());
        list.sort(Comparator.comparingInt(Device::getDeviceId));
        return list;
    }

    public Device getDeviceById(int deviceId) {
        ensureLoaded();
        return cache.get(deviceId);
    }

    public int getDeviceCount() {
        ensureLoaded();
        return cache.size();
    }

    public int getOnlineDeviceCount() {
        ensureLoaded();
        int count = 0;
        for (Device d : cache.values()) {
            if ("ONLINE".equals(d.getStatus())) count++;
        }
        return count;
    }

    /**
     * Update device status IN-MEMORY ONLY. DB sync happens on the slow loop.
     * Marks the device as "dirty" so flushDirtyStatuses() knows to write it.
     */
    public void updateStatusInMemory(int deviceId, String newStatus) {
        Device d = cache.get(deviceId);
        if (d != null && !newStatus.equals(d.getStatus())) {
            d.setStatus(newStatus);
            dirtyDeviceIds.add(deviceId); // mark as changed
        }
    }

    /**
     * Write ONLY the changed device statuses to the database, then clear
     * the dirty set. Called by the 30-second persistence loop.
     *
     * If 0 devices changed status, this method does ZERO database writes.
     */
    public void flushDirtyStatuses() {
        if (dirtyDeviceIds.isEmpty()) return;

        // Snapshot and clear atomically-enough for our use case
        java.util.Set<Integer> snapshot = new java.util.HashSet<>(dirtyDeviceIds);
        dirtyDeviceIds.removeAll(snapshot);

        int flushed = 0;
        for (int id : snapshot) {
            Device d = cache.get(id);
            if (d != null) {
                deviceDAO.updateDeviceStatus(id, d.getStatus());
                flushed++;
            }
        }
        if (flushed > 0) {
            System.out.println("[DeviceCache] Flushed " + flushed + " status changes to DB.");
        }
    }

    /**
     * Called after add/edit/delete device operations to refresh cache.
     */
    public void invalidate() {
        reload();
    }
}
