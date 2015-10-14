package se.dose.dosepics;

/**
 * This is a static class that does nothing but hold the body of a RestService
 * request or response.
 *
 * The reason I chose to do this instead of sending data through Intents is
 * that Intents can not handle more than about one megabyte of data. This can
 * be insufficient at times when sending large image chunks.
 */
public class BodyHolder {

    private static String data;

    public static String getData() {
        return data;
    }

    public static void setData(String newData) {
        data = newData;
    }
}
