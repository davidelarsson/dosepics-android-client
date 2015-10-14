package se.dose.dosepics;

/**
 * Static class that does nothing but hold image data being sent by
 * ShareActivity to UploadImageService.
 * The reason I chose to do this instead of sending data through Intents is
 * that Intents can not handle more than about one megabyte of data. Images
 * being shared are often much larger than that
 */
public class ImageDataHolder {
        private static byte[] data;

    public static byte[] getData() {
        return data;
    }

    public static void setData(byte[] newData) {
        data = newData;
    }
}
