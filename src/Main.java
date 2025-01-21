public class Main {
    public static void main(String[] args) {
        try {
            // Initialize the kernel and scheduler
            Kernel kernel = new Kernel();
            Scheduler scheduler = new Scheduler(kernel);

            // Start the OS
            OS.startup(new Ping(), PCB.Priority.INTERACTIVE, new FakeFileSystem());
            OS.startup(new Pong(), PCB.Priority.INTERACTIVE, new FakeFileSystem());

            // Simulate running the system
            System.out.println("Operating system simulation running...");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("An error occurred while running the OS simulation.");
        }
    }
}
