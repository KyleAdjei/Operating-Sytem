public class KernelMessage {
    private int senderPid;
    private int targetPid;
    private int what;
    private byte[] data;

    // Constructor
    public KernelMessage(int senderPid, int targetPid, int what, byte[] data) {
        this.senderPid = senderPid;
        this.targetPid = targetPid;
        this.what = what;
        this.data = data.clone();
    }

    // Copy constructor
    public KernelMessage(KernelMessage other) {
        this.senderPid = other.senderPid;
        this.targetPid = other.targetPid;
        this.what = other.what;
        this.data = other.data.clone();
    }

    // Getter methods
    public int getSenderPid() {
        return senderPid;
    }

    public void setSenderPid(int senderPid) {
        this.senderPid = senderPid;
    }

    public int getTargetPid() {
        return targetPid;
    }

    public int getWhat() {
        return what;
    }

    public byte[] getData() {
        return data.clone();
    }


    @Override
    public String toString() {
        String dataString = new String(data);
        return "KernelMessage{" +
                "senderPid=" + senderPid +
                ", targetPid=" + targetPid +
                ", messageType=" + what +
                ", data=" + dataString +
                '}';
    }
}