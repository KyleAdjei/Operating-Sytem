import java.util.*;
import java.util.stream.Collectors;
import java.time.Clock;

public class Scheduler {

    private PCB currentlyRunning;
    private final Clock clock = Clock.systemDefaultZone();
    private final LinkedList<PCB> realTimeQueue = new LinkedList<>();
    private final LinkedList<PCB> interactiveQueue = new LinkedList<>();
    private final LinkedList<PCB> backgroundQueue = new LinkedList<>();
    private final LinkedList<PCB> sleepingQueue = new LinkedList<>();
    private final List<PCB> allProcesses = new LinkedList<>();
    private final Random random = new Random();
    private final Kernel kernel;
    private int nextFreeBlock = 0;

    public Scheduler(Kernel kernel) {
        this.kernel = kernel;
    }

    public Scheduler() {
        this.kernel = null;
    }

    public int findFreePhysicalPage() {
        boolean[] freeList = Kernel.getFreelist();
        for (int i = 0; i < freeList.length; i++) {
            if (freeList[i]) {
                freeList[i] = false; // Mark page as used
                return i;
            }
        }
        return -1; // No free page available
    }

    public int handlePageSwap(int virtualPage) {
        PCB victimProcess = getRandomProcess();
        if (victimProcess == null) {
            throw new RuntimeException("No available process for swapping.");
        }
        int victimPageIndex = findVictimPage(victimProcess);
        if (victimPageIndex == -1) {
            throw new RuntimeException("No swappable page found in the selected process.");
        }
        return swapOut(victimProcess, victimPageIndex);
    }

    private int findVictimPage(PCB victimProcess) {
        VirtualToPhysicalMapping[] pageTable = PCB.getPagetable();
        List<Integer> physicalPages = new ArrayList<>();
        for (int i = 0; i < pageTable.length; i++) {
            if (pageTable[i] != null && pageTable[i].physicalPageNumber != -1) {
                physicalPages.add(i);
            }
        }
        if (physicalPages.isEmpty()) {
            return -1;
        }
        return physicalPages.get(random.nextInt(physicalPages.size()));
    }

    private int swapOut(PCB victimProcess, int victimPageIndex) {
        VirtualToPhysicalMapping mapping = PCB.getPagetable()[victimPageIndex];
        if (mapping.onDiskPageNumber == -1) {
            mapping.onDiskPageNumber = allocateNewDiskBlock();
        }
        if (!writePageToDisk(mapping.physicalPageNumber, mapping.onDiskPageNumber)) {
            throw new RuntimeException("Failed to write page to disk during swap.");
        }
        int freedPhysicalPage = mapping.physicalPageNumber;
        mapping.physicalPageNumber = -1;
        return freedPhysicalPage;
    }

    private PCB getRandomProcess() {
        List<PCB> candidates = allProcesses.stream()
                .filter(process -> Arrays.stream(PCB.getPagetable())
                        .anyMatch(mapping -> mapping != null && mapping.physicalPageNumber != -1))
                .collect(Collectors.toList());
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    private int allocateNewDiskBlock() {
        return nextFreeBlock++;
    }

    private boolean writePageToDisk(int physicalPage, int diskPageNumber) {
        try {
            byte[] data = Kernel.getPhysicalMemory(physicalPage);
            VFS.Write(diskPageNumber, data);
            return true;
        } catch (Exception e) {
            System.err.println("Disk write failed: " + e.getMessage());
            return false;
        }
    }

    public int getPid() {
        return currentlyRunning != null ? currentlyRunning.getPid() : -1;
    }

    public int GetPidByName(String name) {
        return allProcesses.stream()
                .filter(process -> process.getName().equals(name))
                .map(PCB::getPid)
                .findFirst()
                .orElse(-1);
    }

    public List<PCB> getAllProcesses() {
        return allProcesses;
    }

    public void sleep(int milliseconds) {
        currentlyRunning.setWakeUpTime(clock.millis() + milliseconds);
        sleepingQueue.add(currentlyRunning);
        switchProcess();
    }

    public void createProcess(PCB process) {
        allProcesses.add(process);
        switch (process.getPriority()) {
            case REAL_TIME -> realTimeQueue.add(process);
            case INTERACTIVE -> interactiveQueue.add(process);
            case BACKGROUND -> backgroundQueue.add(process);
            default -> throw new IllegalArgumentException("Unknown priority: " + process.getPriority());
        }
        if (currentlyRunning == null) {
            switchProcess();
        }
    }

    private void runProcess(PCB process) {
        if (process != null) {
            currentlyRunning = process;
            process.run();
        }
    }

    public void switchProcess() {
        if (currentlyRunning != null && currentlyRunning.isDone()) {
            kernel.closeAllDevicesForProcess(currentlyRunning);
        }

        clearTLB();

        wakeUpSleepingProcesses();

        if (!realTimeQueue.isEmpty() && random.nextInt(10) < 6) {
            runProcess(realTimeQueue.poll());
        } else if (!interactiveQueue.isEmpty() && random.nextInt(10) < 9) {
            runProcess(interactiveQueue.poll());
        } else if (!backgroundQueue.isEmpty()) {
            runProcess(backgroundQueue.poll());
        }
    }

    private void clearTLB() {
        if (currentlyRunning != null) {
            for (int[] entry : UserlandProcess.getTlb()) {
                Arrays.fill(entry, -1);
            }
        }
    }

    private void wakeUpSleepingProcesses() {
        long currentTime = clock.millis();
        Iterator<PCB> iterator = sleepingQueue.iterator();
        while (iterator.hasNext()) {
            PCB process = iterator.next();
            if (process.getWakeUpTime() <= currentTime) {
                iterator.remove();
                RestoreToRunnableQueue(process);
            }
        }
    }

    public void RestoreToRunnableQueue(PCB process) {
        switch (process.getPriority()) {
            case REAL_TIME -> realTimeQueue.add(process);
            case INTERACTIVE -> interactiveQueue.add(process);
            case BACKGROUND -> backgroundQueue.add(process);
            default -> throw new IllegalArgumentException("Unknown priority: " + process.getPriority());
        }
    }

    private PCB.Priority determinePriority(PCB process) {
        final int MAX_EXCEEDANCES = 5;
        if (process.getTimeLimit() > MAX_EXCEEDANCES) {
            process.demotePriority();
        }
        return process.getPriority();
    }

    public PCB getCurrentlyRunning() {
        return currentlyRunning;
    }
}
