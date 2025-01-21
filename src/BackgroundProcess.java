public class BackgroundProcess extends UserlandProcess {
    @Override
    public void main() {
        while (true) {

            System.out.println();
            System.out.println("BackgroundProcess starting...");
            System.out.println();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            OS.sleep(100);
            cooperate();
        }
    }
}