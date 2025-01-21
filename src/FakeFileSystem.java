import java.io.RandomAccessFile;
import java.io.IOException;

public class FakeFileSystem implements Devices {

    private static final int MAX_FILES = 10;
    private RandomAccessFile[] files = new RandomAccessFile[MAX_FILES];

    public int Open(String filename) throws Exception {
        if (filename == null || filename.trim().isEmpty()) {
            throw new Exception("Filename cannot be null or empty.");
        }

        for (int index = 0; index < MAX_FILES; index++) {
            if (files[index] == null) {
                files[index] = new RandomAccessFile(filename, "rw");
                return index;
            }
        }
        return -1; // No available slot
    }

    @Override
    public void Close(int id) {
        if (isValidFile(id)) {
            try {
                files[id].close();
            } catch (IOException e) {
                System.err.println("Error closing file at index " + id + ": " + e.getMessage());
            } finally {
                files[id] = null;
            }
        }
    }

    @Override
    public byte[] Read(int id, int size) {
        if (isValidFile(id) && size > 0) {
            try {
                byte[] buffer = new byte[size];
                int bytesRead = files[id].read(buffer);
                if (bytesRead > 0) {
                    return buffer;
                }
            } catch (IOException e) {
                System.err.println("Error reading from file at index " + id + ": " + e.getMessage());
            }
        }
        return new byte[0]; // Return empty array if read fails
    }

    @Override
    public void Seek(int id, int to) {
        if (isValidFile(id)) {
            try {
                files[id].seek(to);
            } catch (IOException e) {
                System.err.println("Error seeking file at index " + id + ": " + e.getMessage());
            }
        }
    }

    @Override
    public int Write(int id, byte[] data) {
        if (isValidFile(id) && data != null) {
            try {
                files[id].write(data);
                return data.length;
            } catch (IOException e) {
                System.err.println("Error writing to file at index " + id + ": " + e.getMessage());
            }
        }
        return 0;
    }

    /**
     * Utility method to validate if a file ID is valid and open.
     *
     * @param id The file ID to validate
     * @return true if the ID is valid and the file is open
     */
    private boolean isValidFile(int id) {
        return id >= 0 && id < MAX_FILES && files[id] != null;
    }
}
