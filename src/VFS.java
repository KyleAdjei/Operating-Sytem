import java.util.HashMap;
import java.util.Map;

public class VFS {

    // Maps a VFS ID to a devices and its corresponding devices ID
    private static final Map<Integer, DeviceEntry> deviceMap = new HashMap<>();
    private int nextVfsId = 0; // Counter for generating unique VFS IDs

    // Inner class representing an entry in the devices map
    private static class DeviceEntry {
        final Devices devices;
        final int deviceId;

        DeviceEntry(Devices devices, int deviceId) {
            this.devices = devices;
            this.deviceId = deviceId;
        }
    }

    /**
     * Opens a devices based on the input string.
     *
     * @param input A string containing the devices type and optional details.
     * @return A unique VFS ID for the opened devices.
     * @throws Exception if the devices cannot be opened.
     */
    public int Open(String input) throws Exception {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty.");
        }

        // Split the input into devices type and details
        String[] parts = input.split(" ", 2);
        String deviceName = parts[0];
        String details = (parts.length > 1) ? parts[1] : "";

        // Determine and initialize the appropriate devices
        Devices devices = switch (deviceName.toLowerCase()) {
            case "random" -> new RandomDevice();
            case "file" -> new FakeFileSystem();
            default -> throw new IllegalArgumentException("Unsupported devices type: " + deviceName);
        };

        // Open the devices and obtain a devices-specific ID
        int deviceId = devices.Open(details);
        if (deviceId == -1) {
            throw new Exception("Failed to open devices: " + deviceName);
        }

        // Generate a new VFS ID and associate it with the devices
        int vfsId = nextVfsId++;
        deviceMap.put(vfsId, new DeviceEntry(devices, deviceId));
        return vfsId;
    }

    /**
     * Closes the devices associated with the given VFS ID.
     *
     * @param vfsId The VFS ID of the devices to close.
     */
    public void Close(int vfsId) {
        DeviceEntry entry = deviceMap.get(vfsId);
        if (entry != null) {
            entry.devices.Close(entry.deviceId); // Close the devices
            deviceMap.remove(vfsId); // Remove the entry from the map
        } else {
            throw new IllegalArgumentException("Invalid VFS ID: " + vfsId);
        }
    }

    /**
     * Reads data from the devices associated with the given VFS ID.
     *
     * @param vfsId The VFS ID of the devices to read from.
     * @param size The number of bytes to read.
     * @return A byte array containing the read data.
     */
    public static byte[] Read(int vfsId, int size) {
        DeviceEntry entry = deviceMap.get(vfsId);
        if (entry != null) {
            return entry.devices.Read(entry.deviceId, size); // Perform the read operation
        } else {
            throw new IllegalArgumentException("Invalid VFS ID: " + vfsId);
        }
    }

    /**
     * Writes data to the devices associated with the given VFS ID.
     *
     * @param vfsId The VFS ID of the devices to write to.
     * @param data The data to write.
     * @return The number of bytes written.
     */
    public static int Write(int vfsId, byte[] data) {
        DeviceEntry entry = deviceMap.get(vfsId);
        if (entry != null) {
            return entry.devices.Write(entry.deviceId, data); // Perform the write operation
        } else {
            throw new IllegalArgumentException("Invalid VFS ID: " + vfsId);
        }
    }

    /**
     * Seeks to a specific position in the devices associated with the given VFS ID.
     *
     * @param vfsId The VFS ID of the devices to seek in.
     * @param to The position to seek to.
     */
    public void Seek(int vfsId, int to) {
        DeviceEntry entry = deviceMap.get(vfsId);
        if (entry != null) {
            entry.devices.Seek(entry.deviceId, to); // Perform the seek operation
        } else {
            throw new IllegalArgumentException("Invalid VFS ID: " + vfsId);
        }
    }
}
