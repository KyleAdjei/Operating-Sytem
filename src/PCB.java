import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

public class PCB {
    private static int nextPid = 0;
    private final int pid;
    private final UserlandProcess userlandProcess;
    private long wakeUpTime = Long.MAX_VALUE;
    private Priority priority;
    private int timeLimit = 0;
    private final int[] deviceIds = new int[10];
    private String name;
    private final LinkedList<KernelMessage> messageQueue = new LinkedList<>();
    private boolean waitingForMessage = false;
    private static final int PAGE_SIZE = 1024;
    private static final VirtualToPhysicalMapping[] pageTable = new VirtualToPhysicalMapping[100];

    static {
        // Initialize page table entries
        for (int i = 0; i < pageTable.length; i++) {
            pageTable[i] = new VirtualToPhysicalMapping();
        }
    }

    public PCB(UserlandProcess userlandProcess) {
        this.userlandProcess = userlandProcess;
        this.pid = nextPid++;
        Arrays.fill(deviceIds, -1);
        this.name = userlandProcess.getClass().getSimpleName();
    }

    public PCB(UserlandProcess userlandProcess, Priority priority) {
        this(userlandProcess);
        this.priority = priority;
    }

    public static VirtualToPhysicalMapping[] getPagetable() {
        return pageTable;
    }

    public static VirtualToPhysicalMapping getMapping(int virtualPage, Scheduler scheduler) {
        VirtualToPhysicalMapping mapping = pageTable[virtualPage];

        if (mapping.physicalPageNumber == -1) {
            int physicalPage = scheduler.findFreePhysicalPage();

            if (physicalPage == -1) {
                physicalPage = scheduler.handlePageSwap(virtualPage);
            }

            mapping.physicalPageNumber = physicalPage;

            if (mapping.onDiskPageNumber != -1) {
                loadDataFromDisk(mapping.onDiskPageNumber, physicalPage);
            } else {
                initializePhysicalPage(physicalPage);
            }
        }

        updateTLB(virtualPage, mapping.physicalPageNumber);
        return mapping;
    }

    public static void initializePhysicalPage(int physicalPage) {
        VirtualToPhysicalMapping mapping = pageTable[physicalPage];

        if (mapping != null) {
            mapping.isInitialized = true;
        } else {
            throw new IllegalArgumentException("Invalid physical page index: " + physicalPage);
        }
    }

    public static void loadDataFromDisk(int diskPageNumber, int physicalPage) {
        int offset = diskPageNumber * PAGE_SIZE;

        try {
            byte[] data = VFS.Read(offset, PAGE_SIZE);

            if (data.length != PAGE_SIZE) {
                throw new IOException("Insufficient data read from disk.");
            }

            System.arraycopy(data, 0, Kernel.getPhysicalMemory(physicalPage), 0, PAGE_SIZE);
        } catch (IOException e) {
            System.err.println("Disk read error: " + e.getMessage());
        }
    }

    private static void updateTLB(int virtualPage, int physicalPage) {
        Random random = new Random();
        int tlbIndex = random.nextInt(2);
        UserlandProcess.getTlb()[tlbIndex][0] = virtualPage;
        UserlandProcess.getTlb()[tlbIndex][1] = physicalPage;
    }

    public boolean isWaitingForMessage() {
        return waitingForMessage;
    }

    public void setWaitingForMessage(boolean waiting) {
        this.waitingForMessage = waiting;
    }

    public void addMessage(KernelMessage message) {
        this.messageQueue.add(message);
    }

    public synchronized KernelMessage dequeueMessage() {
        return messageQueue.poll();
    }

    public LinkedList<KernelMessage> getMessageQueue() {
        return messageQueue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int[] getDeviceIds() {
        return deviceIds;
    }

    public UserlandProcess getUlp() {
        return userlandProcess;
    }

    public int getPid() {
        return pid;
    }

    public long getWakeUpTime() {
        return wakeUpTime;
    }

    public void setWakeUpTime(long wakeUpTime) {
        this.wakeUpTime = wakeUpTime;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void incrementTimeLimit() {
        timeLimit++;
    }

    public void resetTimeSliceExceedances() {
        timeLimit = 0;
    }

    public void demotePriority() {
        if (priority == Priority.REAL_TIME) {
            priority = Priority.INTERACTIVE;
        } else if (priority == Priority.INTERACTIVE) {
            priority = Priority.BACKGROUND;
        }
        System.out.println("Priority demoted for process " + pid);
        resetTimeSliceExceedances();
    }

    public void stop() {
        userlandProcess.stop();
        while (!userlandProcess.isStopped()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isDone() {
        return userlandProcess.isDone();
    }

    public void run() {
        userlandProcess.start();
    }

    public enum Priority {
        REAL_TIME, INTERACTIVE, BACKGROUND
    }
}
