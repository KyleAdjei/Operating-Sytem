import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class Kernel implements Runnable, Devices {
    private final Thread kernelThread;
    private final Semaphore signal = new Semaphore(0);
    private final Scheduler processScheduler;
    private final VFS virtualFileSystem;
    private final FakeFileSystem fileManager;
    private final Map<Integer, PCB> waitingProcesses = new HashMap<>();
    private final int[] activeDevices;
    private static final boolean[] freeList = new boolean[1024];
    private static final int PAGE_SIZE = 1024;
    private static final String SWAP_FILE = "swapfile.swap";
    private int swapFileDescriptor;

    public Kernel() {
        this.kernelThread = new Thread(this);
        this.processScheduler = new Scheduler(this);
        this.virtualFileSystem = new VFS();
        this.fileManager = new FakeFileSystem();
        this.activeDevices = new int[1024];
        Arrays.fill(freeList, true);
        initializeSwapFile();
    }

    public static boolean[] getFreelist() {
        return freeList;
    }

    public void start() {
        signal.release();
        if (!kernelThread.isAlive()) {
            kernelThread.start();
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                signal.acquire();
                handleSystemCall();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void initializeSwapFile() {
        try {
            swapFileDescriptor = fileManager.Open(SWAP_FILE);
            if (swapFileDescriptor == -1) {
                throw new RuntimeException("Unable to open swap file.");
            }
        } catch (Exception e) {
            System.err.println("Error during swap file initialization: " + e.getMessage());
        }
    }

    public static byte[] getPhysicalMemory(int physicalPage) {
        VirtualToPhysicalMapping mapping = PCB.getPagetable()[physicalPage];
        if (mapping != null && mapping.data != null) {
            return mapping.data;
        }
        throw new IllegalStateException("No valid data for page: " + physicalPage);
    }

    public int AllocateMemory(int size) {
        int pagesNeeded = size / PAGE_SIZE;
        for (int i = 0; i <= PCB.getPagetable().length - pagesNeeded; i++) {
            if (arePagesFree(i, pagesNeeded)) {
                for (int j = 0; j < pagesNeeded; j++) {
                    PCB.getPagetable()[i + j] = new VirtualToPhysicalMapping();
                }
                return i * PAGE_SIZE; // Virtual memory address
            }
        }
        return -1; // Insufficient memory
    }

    private boolean arePagesFree(int start, int count) {
        for (int i = start; i < start + count; i++) {
            if (PCB.getPagetable()[i] != null) {
                return false;
            }
        }
        return true;
    }

    public boolean FreeMemory(int pointer, int size) {
        int startPage = pointer / PAGE_SIZE;
        int pageCount = size / PAGE_SIZE;

        for (int i = 0; i < pageCount; i++) {
            int pageIndex = startPage + i;
            VirtualToPhysicalMapping mapping = PCB.getPagetable()[pageIndex];
            if (mapping != null) {
                if (mapping.physicalPageNumber != -1) {
                    freeList[mapping.physicalPageNumber] = true;
                }
                PCB.getPagetable()[pageIndex] = null;
            }
        }
        return true;
    }

    public int getPid() {
        return processScheduler.getPid();
    }

    public int GetPidByName(String name) {
        return processScheduler.GetPidByName(name);
    }

    public void sendMessage(KernelMessage message) {
        KernelMessage messageCopy = new KernelMessage(message);
        messageCopy.setSenderPid(getPid());
        PCB target = processScheduler.getAllProcesses().get(messageCopy.getTargetPid());

        if (target != null) {
            target.getMessageQueue().add(messageCopy);
            if (target.isWaitingForMessage()) {
                restoreProcess(target);
            }
        } else {
            System.out.println("Target process does not exist.");
        }
    }

    public void restoreProcess(PCB process) {
        processScheduler.RestoreToRunnableQueue(process);
    }

    public KernelMessage WaitForMessage() {
        PCB currentProcess = processScheduler.getCurrentlyRunning();

        if (!currentProcess.getMessageQueue().isEmpty()) {
            return currentProcess.getMessageQueue().remove();
        }

        descheduleCurrentProcess();
        waitingProcesses.put(currentProcess.getPid(), currentProcess);

        while (currentProcess.isWaitingForMessage()) {
            processScheduler.switchProcess();
        }

        return currentProcess.getMessageQueue().remove();
    }

    private void descheduleCurrentProcess() {
        PCB runningProcess = processScheduler.getCurrentlyRunning();
        if (runningProcess != null) {
            runningProcess.setWaitingForMessage(true);
        } else {
            System.out.println("No running process to deschedule.");
        }
    }

    private void handleSystemCall() {
        switch (OS.currentCall) {
            case CREATE_PROCESS -> {
                PCB process = (PCB) OS.parameters.get(0);
                processScheduler.createProcess(process);
            }
            case SWITCH_PROCESS -> processScheduler.switchProcess();
            case SLEEP -> {
                int milliseconds = (Integer) OS.parameters.get(0);
                processScheduler.sleep(milliseconds);
            }
            default -> throw new IllegalStateException("Unknown system call: " + OS.currentCall);
        }
    }

    @Override
    public int Open(String details) throws Exception {
        PCB currentProcess = processScheduler.getCurrentlyRunning();
        int[] deviceIds = currentProcess.getDeviceIds();

        for (int i = 0; i < deviceIds.length; i++) {
            if (deviceIds[i] == -1) {
                int vfsId = virtualFileSystem.Open(details);
                if (vfsId != -1) {
                    deviceIds[i] = vfsId;
                    return i;
                }
                return -1;
            }
        }
        return -1;
    }

    public void Close(int id) {
        PCB currentProcess = processScheduler.getCurrentlyRunning();
        int vfsId = currentProcess.getDeviceIds()[id];
        if (vfsId != -1) {
            virtualFileSystem.Close(vfsId);
            currentProcess.getDeviceIds()[id] = -1;
        }
    }

    @Override
    public byte[] Read(int id, int size) {
        PCB currentProcess = processScheduler.getCurrentlyRunning();
        int vfsId = currentProcess.getDeviceIds()[id];
        return vfsId != -1 ? VFS.Read(vfsId, size) : new byte[0];
    }

    @Override
    public void Seek(int id, int to) {
        PCB currentProcess = processScheduler.getCurrentlyRunning();
        int vfsId = currentProcess.getDeviceIds()[id];
        if (vfsId != -1) {
            virtualFileSystem.Seek(vfsId, to);
        }
    }

    @Override
    public int Write(int id, byte[] data) {
        PCB currentProcess = processScheduler.getCurrentlyRunning();
        int vfsId = currentProcess.getDeviceIds()[id];
        return vfsId != -1 ? VFS.Write(vfsId, data) : -1;
    }

    public void closeAllDevicesForProcess(PCB process) {
        int[] deviceIds = process.getDeviceIds();
        for (int i = 0; i < deviceIds.length; i++) {
            if (deviceIds[i] != -1) {
                virtualFileSystem.Close(deviceIds[i]);
                deviceIds[i] = -1;
            }
        }
        System.out.println("Devices for process " + process.getPid() + " closed.");
    }
}
