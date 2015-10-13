package se.dose.dosepics;

public class ImageDataHolder {
        private static byte[] data;

    public static byte[] getData() {
        return data;
    }

    public static void setData(byte[] newData) {
        data = newData;
    }
}
