public class GoodbyeWorld extends UserlandProcess {
    @Override
    public void main() {
        while (true) {
            System.out.println("Goodbye world");
            OS.sleep(50);
            try {
                Thread.sleep(50); // To prevent flooding the console too quickly
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            cooperate();
        }
    }
}