import java.util.ArrayList;

public class OS {
    private static Kernel kernelInstance;
    private static Scheduler processScheduler;
    public static CallType currentCall;
    public static final ArrayList<Object> parameters = new ArrayList<>();
    public static Object returnValue;

    public enum CallType {
        CREATE_PROCESS, SWITCH_PROCESS, SLEEP
    }

    static {
        processScheduler = new Scheduler();
    }

    public static int AllocateMemory(int size) {
        if (size % 1024 == 0) {
            return kernelInstance.AllocateMemory(size);
        }
        return -1; // Invalid size
    }

    public static boolean FreeMemory(int pointer, int size) {
        if (pointer % 1024 != 0 || size % 1024 != 0) {
            return false;
        }
        return kernelInstance.FreeMemory(pointer, size);
    }

    public static void getMapping(int virtualPage) {
        PCB.getMapping(virtualPage, processScheduler);
    }

    public static int getPid() {
        return kernelInstance.getPid();
    }

    public static int GetPidByName(String name) {
        return kernelInstance.GetPidByName(name);
    }

    public static void sendMessage(KernelMessage message) {
        kernelInstance.sendMessage(message);
    }

    public static KernelMessage waitForMessage() {
        return kernelInstance.WaitForMessage();
    }

    public static void sleep(int milliseconds) {
        prepareSystemCall(CallType.SLEEP, milliseconds);
        switchToKernel();
    }

    public static int createProcess(UserlandProcess process) {
        return createProcess(process, PCB.Priority.INTERACTIVE); // Default priority
    }

    public static int createProcess(UserlandProcess process, PCB.Priority priority) {
        PCB pcb = new PCB(process, priority);
        prepareSystemCall(CallType.CREATE_PROCESS, pcb);

        switchToKernel();
        System.out.println("Process created with PID: " + pcb.getPid());
        return pcb.getPid();
    }

    public static void startup(UserlandProcess process, PCB.Priority priority, FakeFileSystem fileSystem) {
        initializeKernel();
        createProcess(process, priority);
    }

    public static void startup(UserlandProcess process) {
        initializeKernel();
        createProcess(process, PCB.Priority.INTERACTIVE);
    }

    private static void initializeKernel() {
        kernelInstance = new Kernel();
    }

    private static void prepareSystemCall(CallType call, Object... args) {
        parameters.clear();
        currentCall = call;
        for (Object arg : args) {
            parameters.add(arg);
        }
    }

    private static void switchToKernel() {
        kernelInstance.start();

        PCB currentProcess = processScheduler.getCurrentlyRunning();
        if (currentProcess != null) {
            currentProcess.stop();
        }

        if (currentProcess == null) {
            try {
                Thread.sleep(10); // Prevent busy-waiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void switchProcess() {
        prepareSystemCall(CallType.SWITCH_PROCESS);
        switchToKernel();
    }

    public static int open(String deviceName) throws Exception {
        return kernelInstance.Open(deviceName);
    }

    public static byte[] read(int deviceId, int size) {
        return kernelInstance.Read(deviceId, size);
    }

    public static int write(int deviceId, byte[] data) {
        return kernelInstance.Write(deviceId, data);
    }

    public static void seek(int deviceId, int position) {
        kernelInstance.Seek(deviceId, position);
    }

    public static void close(int deviceId) {
        kernelInstance.Close(deviceId);
    }
}
