import java.util.Random;

public class RandomDevice implements Devices {

    // Array to manage Random instances for each opened device
    private final Random[] randomDevices = new Random[10];

    @Override
    public int Open(String seed) {
        for (int i = 0; i < randomDevices.length; i++) {
            if (randomDevices[i] == null) {
                // Initialize a new Random instance with or without a seed
                randomDevices[i] = createRandomInstance(seed);
                return i; // Return the index of the newly opened device
            }
        }
        return -1; // Return -1 if no slots are available
    }

    @Override
    public void Close(int id) {
        if (isValidDeviceId(id)) {
            randomDevices[id] = null; // Close the device by nullifying its Random instance
        }
    }

    @Override
    public byte[] Read(int id, int size) {
        if (isValidDeviceId(id) && randomDevices[id] != null) {
            byte[] buffer = new byte[size];
            randomDevices[id].nextBytes(buffer); // Populate the buffer with random bytes
            return buffer;
        }
        return null; // Return null if the device ID is invalid or uninitialized
    }

    @Override
    public void Seek(int id, int to) {
        if (isValidDeviceId(id) && randomDevices[id] != null) {
            byte[] buffer = new byte[to];
            randomDevices[id].nextBytes(buffer); // Generate random bytes to simulate seeking
        }
    }

    @Override
    public int Write(int id, byte[] data) {
        // Random devices are read-only; writing is not supported
        return 0;
    }

    /**
     * Helper method to create a Random instance, optionally seeded.
     *
     * @param seed the seed as a string, or null for an unseeded instance
     * @return a new Random instance
     */
    private Random createRandomInstance(String seed) {
        if (seed != null && !seed.isEmpty()) {
            try {
                // Attempt to parse the seed and initialize Random with it
                int parsedSeed = Integer.parseInt(seed);
                return new Random(parsedSeed);
            } catch (NumberFormatException e) {
                // If the seed is not a valid integer, fall back to default constructor
            }
        }
        return new Random();
    }

    /**
     * Helper method to validate device IDs.
     *
     * @param id the device ID to validate
     * @return true if the ID is valid, false otherwise
     */
    private boolean isValidDeviceId(int id) {
        return id >= 0 && id < randomDevices.length;
    }
}
