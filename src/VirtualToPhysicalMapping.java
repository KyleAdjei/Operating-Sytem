

public class VirtualToPhysicalMapping {

    public byte[] data;
    public int physicalPageNumber;
    public int onDiskPageNumber;
    public boolean isInitialized;

    public VirtualToPhysicalMapping() {
        this.physicalPageNumber = -1;
        this.onDiskPageNumber = -1;
        this.isInitialized = false;
    }

    public int getPhysicalPageNumber() {
        return physicalPageNumber;
    }

    public void setPhysicalPageNumber(int physicalPageNumber) {
        this.physicalPageNumber = physicalPageNumber;
    }

    public int getOnDiskPageNumber() {
        return onDiskPageNumber;
    }

    public void setOnDiskPageNumber(int onDiskPageNumber) {
        this.onDiskPageNumber = onDiskPageNumber;
    }

}