public class Pong extends UserlandProcess {
    @Override
    public void main() throws InterruptedException {
        while (true) {
            System.out.println("I am PONG");

            // Wait for a message from another process
            KernelMessage incomingMessage = OS.waitForMessage();

            // Log the received message details
            if (incomingMessage.getWhat() == 0) {
                System.out.println("I am PONG, ping received from PID: " + incomingMessage.getSenderPid());
            }

            System.out.printf(
                    "  PONG: from: %d to: %d what: %d%n",
                    incomingMessage.getSenderPid(),
                    OS.getPid(),
                    incomingMessage.getWhat()
            );

            // Prepare a reply to the sender
            KernelMessage replyMessage = new KernelMessage(
                    OS.getPid(),
                    incomingMessage.getSenderPid(),
                    incomingMessage.getWhat(),
                    new byte[0] // Empty payload
            );

            // Send the reply back
            OS.sendMessage(replyMessage);

            // Yield control to allow other processes to execute
            cooperate();
        }
    }
}
