public class Ping extends UserlandProcess {
    @Override
    public void main() throws Exception {
        // Retrieve Pong process PID
        int pongPid = OS.GetPidByName("Pong");
        System.out.println("I am PING, Pong PID = " + pongPid);

        // Handle case where Pong process is not found
        if (pongPid == -1) {
            System.out.println("Pong process not found. Exiting.");
            return;
        }

        // Communicate with Pong process 5 times
        for (int i = 0; i < 5; i++) {
            // Send a message to Pong
            KernelMessage outgoingMessage = new KernelMessage(OS.getPid(), pongPid, i, new byte[0]);
            OS.sendMessage(outgoingMessage);

            // Sleep briefly before awaiting a response
            OS.sleep(20);

            // Wait for a response from Pong
            KernelMessage incomingMessage = OS.waitForMessage();

            // Log the details of the received message
            System.out.printf(
                    "  PING: from: %d to: %d what: %d%n",
                    incomingMessage.getSenderPid(),
                    OS.getPid(),
                    incomingMessage.getWhat()
            );

            // Yield control to allow other processes to execute
            cooperate();
        }
    }
}
