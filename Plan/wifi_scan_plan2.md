## Goal Description
The previous fix to trigger a hardware Wi-Fi scan failed to work properly because the application passes a complex, multi-line C# script directly through the command line via `powershell.exe -Command`. In Java on Windows, passing multi-line strings with quotes to the command prompt often breaks formatting, meaning the PowerShell script never actually ran to force the scan.

The goal is to fix this execution issue so the hardware scan executes successfully.

## User Review Required
> [!NOTE] 
> I will change the logic so that instead of trying to pass the entire script as a single command-line argument, the application will write the script to a temporary file (`wifi_scan.ps1`) in the system's temporary directory, execute that file, and then delete it. This is a much more robust way to run PowerShell scripts from Java.

## Open Questions
None. 

## Proposed Changes
We will modify the `forceWindowsWifiScan()` method in `NetworkAdapter.java`.

### `NetworkAdapter.java`
#### [MODIFY] `NetworkAdapter.java`
We will replace the current implementation of `forceWindowsWifiScan()` with one that writes to a temporary file.

```java
    /**
     * Forces a hardware Wi-Fi scan using the Windows Native WiFi API via a temporary PowerShell script.
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

        java.io.File tempFile = null;
        try {
            tempFile = java.io.File.createTempFile("wifi_scan", ".ps1");
            java.nio.file.Files.write(tempFile.toPath(), script.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            executeCommandArray(new String[]{"powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", tempFile.getAbsolutePath()});
            
            // The hardware scan takes a few seconds to complete and update the Windows cache
            Thread.sleep(3500); 
        } catch (Exception e) {
            System.err.println("Error forcing WiFi scan: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
```

## Verification Plan
### Manual Verification
1. I will apply these changes and recompile `NetworkAdapter.java`.
2. I will ask you to try the Wi-Fi scanner in the app again to confirm it successfully updates without relying on the Windows taskbar menu.
