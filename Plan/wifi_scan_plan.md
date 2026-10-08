## Goal Description
The problem is that the app's Wi-Fi scanning feature currently relies on the `netsh wlan show networks` command. On Windows, this command only returns **cached** results from the last time Windows performed a hardware scan (for example, when you click the Wi-Fi icon in the taskbar). Because of this, the app does not show newly available networks unless you manually trigger a refresh from the Windows UI.

The goal is to programmatically force a hardware Wi-Fi scan directly through the app before retrieving the network list, so the app always displays up-to-date networks without relying on the Windows taskbar.

## User Review Required
> [!NOTE] 
> The hardware Wi-Fi scan takes about 3-4 seconds to complete. I will add a 3.5-second delay to the scanning function to ensure the hardware has enough time to find all networks and update the Windows cache. Because your application already runs the scan in a background thread (`SwingWorker` in `DevicePanel.java`), this delay will **not** freeze the user interface. The UI will just show the "📡 Scanning nearby networks... (This may take a few seconds)" message a bit longer.

## Open Questions
None. The proposed solution has been verified to work on your system.

## Proposed Changes
We will modify the `NetworkAdapter.java` utility to execute a short inline C# script via PowerShell that calls the Windows Native WiFi API (`wlanapi.dll`) to trigger a true hardware scan.

### `NetworkAdapter.java`
#### [MODIFY] `NetworkAdapter.java`
1. Add a new `forceWindowsWifiScan()` method that executes the hardware scan API.
2. Update the `scanWindowsWifi()` method to call `forceWindowsWifiScan()` before retrieving the network list.

```java
    /**
     * Forces a hardware Wi-Fi scan using the Windows Native WiFi API.
     */
    private static void forceWindowsWifiScan() {
        String script = 
            "$code = @\\\"\\n" +
            "using System;\\n" +
            "using System.Runtime.InteropServices;\\n" +
            "public class NativeWifiScan {\\n" +
            "    [DllImport(\\"Wlanapi.dll\\")]\\n" +
            "    public static extern uint WlanOpenHandle(uint dwClientVersion, IntPtr pReserved, out uint pdwNegotiatedVersion, out IntPtr phClientHandle);\\n" +
            "    [DllImport(\\"Wlanapi.dll\\")]\\n" +
            "    public static extern uint WlanEnumInterfaces(IntPtr hClientHandle, IntPtr pReserved, out IntPtr ppInterfaceList);\\n" +
            "    [DllImport(\\"Wlanapi.dll\\")]\\n" +
            "    public static extern uint WlanScan(IntPtr hClientHandle, ref Guid pInterfaceGuid, IntPtr pDot11Ssid, IntPtr pIeData, IntPtr pReserved);\\n" +
            "    [DllImport(\\"Wlanapi.dll\\")]\\n" +
            "    public static extern void WlanFreeMemory(IntPtr pMemory);\\n" +
            "    [DllImport(\\"Wlanapi.dll\\")]\\n" +
            "    public static extern uint WlanCloseHandle(IntPtr hClientHandle, IntPtr pReserved);\\n" +
            "    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]\\n" +
            "    public struct WLAN_INTERFACE_INFO {\\n" +
            "        public Guid InterfaceGuid;\\n" +
            "        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 256)]\\n" +
            "        public string strInterfaceDescription;\\n" +
            "        public uint isState;\\n" +
            "    }\\n" +
            "    public static void Scan() {\\n" +
            "        uint negotiatedVersion;\\n" +
            "        IntPtr clientHandle;\\n" +
            "        if (WlanOpenHandle(2, IntPtr.Zero, out negotiatedVersion, out clientHandle) != 0) return;\\n" +
            "        IntPtr interfaceListPtr;\\n" +
            "        if (WlanEnumInterfaces(clientHandle, IntPtr.Zero, out interfaceListPtr) == 0) {\\n" +
            "            uint numItems = (uint)Marshal.ReadInt32(interfaceListPtr);\\n" +
            "            IntPtr infoPtr = new IntPtr(interfaceListPtr.ToInt64() + 8);\\n" +
            "            for (int i = 0; i < numItems; i++) {\\n" +
            "                WLAN_INTERFACE_INFO info = (WLAN_INTERFACE_INFO)Marshal.PtrToStructure(infoPtr, typeof(WLAN_INTERFACE_INFO));\\n" +
            "                WlanScan(clientHandle, ref info.InterfaceGuid, IntPtr.Zero, IntPtr.Zero, IntPtr.Zero);\\n" +
            "                infoPtr = new IntPtr(infoPtr.ToInt64() + Marshal.SizeOf(typeof(WLAN_INTERFACE_INFO)));\\n" +
            "            }\\n" +
            "            WlanFreeMemory(interfaceListPtr);\\n" +
            "        }\\n" +
            "        WlanCloseHandle(clientHandle, IntPtr.Zero);\\n" +
            "    }\\n" +
            "}\\n" +
            "\\\"@\\n" +
            "Add-Type -TypeDefinition $code -Language CSharp\\n" +
            "[NativeWifiScan]::Scan()\\n";

        try {
            executeCommandArray(new String[]{"powershell.exe", "-NoProfile", "-Command", script});
            // The hardware scan takes a few seconds to complete and update the Windows cache
            Thread.sleep(3500); 
        } catch (Exception e) {
            System.err.println("Error forcing WiFi scan: " + e.getMessage());
        }
    }

    /**
     * Scan WiFi networks on Windows using netsh
     */
    private static List<AvailableNetwork> scanWindowsWifi() {
        // Force a hardware scan before retrieving the cached results
        forceWindowsWifiScan();

        List<AvailableNetwork> networks = new ArrayList<>();
        // ... (rest of the method remains the same)
```

## Verification Plan
### Manual Verification
1. I will apply the changes to the code.
2. I will ask you to build/run the app, open the Wi-Fi scanner, and click the "Scan Again" button.
3. You can verify that new networks appear (e.g., if you turn on a mobile hotspot right before clicking Scan Again) without needing to open the Windows taskbar Wi-Fi menu first.
