import java.util.concurrent.Semaphore;

public abstract class UserlandProcess implements Runnable {
    private final Thread thread;
    private final Semaphore semaphore = new Semaphore(0);
    private boolean quantumExpired = false;

    private static final int PAGE_SIZE = 1024;
    private static final byte[] memory = new byte[1024 * 1024]; // 1 MB memory
    private static final int[][] TLB = { { -1, -1 }, { -1, -1 } }; // TLB: {virtualPage, physicalPage}

    public UserlandProcess() {
        this.thread = new Thread(this);
    }

    public static int[][] getTlb() {
        return TLB;
    }

    /**
     * Reads a byte from the given virtual address.
     * Handles TLB misses by fetching the required mapping.
     *
     * @param address the virtual address to read from
     * @return the byte value at the specified address
     */
    public byte Read(int address) {
        int virtualPage = address / PAGE_SIZE;
        int offset = address % PAGE_SIZE;

        for (int[] entry : TLB) {
            if (entry[0] == virtualPage) { // TLB hit
                int physicalPage = entry[1];
                return memory[physicalPage * PAGE_SIZE + offset];
            }
        }

        // TLB miss: Fetch mapping and retry
        OS.getMapping(virtualPage);
        return Read(address);
    }

    /**
     * Writes a byte to the given virtual address.
     * Handles TLB misses by fetching the required mapping.
     *
     * @param address the virtual address to write to
     * @param value the byte value to write
     */
    public void Write(int address, byte value) {
        int virtualPage = address / PAGE_SIZE;
        int offset = address % PAGE_SIZE;

        for (int[] entry : TLB) {
            if (entry[0] == virtualPage) { // TLB hit
                int physicalPage = entry[1];
                memory[physicalPage * PAGE_SIZE + offset] = value;
                return;
            }
        }

        // TLB miss: Fetch mapping and retry
        OS.getMapping(virtualPage);
        Write(address, value);
    }

    /**
     * Starts the userland process, releasing the semaphore to allow execution.
     */
    public void start() {
        semaphore.release();
        if (!thread.isAlive()) {
            thread.start();
        }
    }

    /**
     * Requests the process to stop at the next cooperative point.
     */
    public void requestStop() {
        quantumExpired = true;
    }

    /**
     * Stops the process by acquiring the semaphore.
     */
    public void stop() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Indicates whether the process has been stopped.
     *
     * @return true if the process is stopped, false otherwise
     */
    public boolean isStopped() {
        return semaphore.availablePermits() == 0;
    }

    /**
     * Indicates whether the process has finished execution.
     *
     * @return true if the process is done, false otherwise
     */
    public boolean isDone() {
        return !thread.isAlive();
    }

    /**
     * Cooperative multitasking: Yields control if the quantum has expired.
     */
    protected void cooperate() {
        if (quantumExpired) {
            quantumExpired = false;
            OS.switchProcess();
        }
    }

    @Override
    public void run() {
        try {
            semaphore.acquire(); // Wait until start is called
            main(); // Execute the main method of the process
        } catch (Exception e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * The main logic of the process, to be implemented by subclasses.
     *
     * @throws Exception if an error occurs during execution
     */
    public abstract void main() throws Exception;
}
