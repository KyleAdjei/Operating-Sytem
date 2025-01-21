public class RealTimeProcess extends UserlandProcess {
    @Override
    public void main() {
        while (true) {
            System.out.println("RealTimeProcess executing...");
            try {
                Thread.sleep(50);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            OS.sleep(100);
            cooperate();


        }
    }
}