package se.dose.dosepics;

import android.app.NotificationManager;
import android.app.Service;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import org.json.JSONObject;
import java.nio.ByteBuffer;

/**
 * UploadImageService reads image data from ImageDataHolder, divides this data
 * into chunks and uses RestService to upload the chunks to the server
 * according to the DOSEPICS REST API specification.
 *
 * If the image is large enough to be divided into chunks (as most images
 * are), the cookie received after the first chunk has been successfully
 * uploaded is sent along subsequent chunk uploads. This way we don't need
 * to authenticate ourselves during each chunk upload.
 */
public class UploadImageService extends Service {

    // The one and only action offered
    public static final String ACTION_UPLOAD = "se.dose.dosepics.action.UPLOAD";

    // Extra arguments
    public static final String EXTRA_DESCRIPTION = "se.dose.dosepics.extra.DESCRIPTION";
    public static final String EXTRA_OWNER = "se.dose.dosepics.extra.OWNER";

    // IDs of Notifications
    private static final int NOTE_ID_PROGRESS = 42;
    private static final int NOTE_ID_ERROR = 43;

    // The only HTTP response codes that we wish to receive
    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;

    // Size of chunks to divide image into
    private static final int CHUNK_SIZE = 500000;

    // Current state
    private enum NetworkState { NETWORK_IDLE, NETWORK_WAIT_FOR_CREATED, NETWORK_WAIT_FOR_OK };
    NetworkState networkState = NetworkState.NETWORK_IDLE;
    private int chunks = 0;
    private int currentChunk = 0;

    // Notification
    NotificationCompat.Builder foregroundBuilder = null;
    Notification foregroundNot = null;

    // Information to be sent to RestService
    private String description = null;
    private String owner = null;
    private String cookie = null;

    NotificationManager notificationManager = null;

    /**
     * Static help function to start an upload
     */
    public static void startActionUpload(Context context, String owner, String description) {

        Intent intent = new Intent(context, UploadImageService.class);
        intent.setAction(ACTION_UPLOAD);
        intent.putExtra(EXTRA_OWNER, owner);
        intent.putExtra(EXTRA_DESCRIPTION, description);
        context.startService(intent);
    }

    /**
     * Register the communication channel with RestService
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // Allow us to receive a message from RestService, so that we
        // can update the notification in the status bar
        LocalBroadcastManager.getInstance(this).registerReceiver(restReceiver, new IntentFilter(RestService.INTENT_FILTER));
        networkState = NetworkState.NETWORK_IDLE;
    }

    /*
     * Receives broadcasts from RestService
     */
    private BroadcastReceiver restReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.hasExtra(RestService.REST_RESPONSE_CODE)) {
                handleResponseCode(intent);
            } else if (intent.hasExtra(RestService.REST_RESPONSE_COOKIE)) {
                handleCookie(intent);
            } else if (intent.hasExtra(RestService.REST_RESPONSE_BODY)) {
                handleResponseBody(intent);
            }
        }
    };

    /**
     * When the upload is done, update the notification in the action bar to
     * reflect this status, wait 3000 ms, and then remove the notification
     */
    private void createDoneNotification()
    {
        foregroundBuilder.setContentText("DONE!");

        // Make sure the progress bar really is at the end!
        foregroundBuilder.setProgress(100, 100, false);

        // Update notification
        notificationManager.notify(NOTE_ID_PROGRESS, foregroundBuilder.build());

        // Wait 3000 ms before stopping service and removing the notification
        int toWait = 3000;
        long endTime = System.currentTimeMillis() + toWait;
        while (System.currentTimeMillis() < endTime) {
            try {
                wait(100);
            } catch (Exception e) {
            }
        }
        stopSelf();
    }

    /**
     * If the upload failed for some reason, create a new notification that
     * informs the user about it and leave it behind when the Service finishes
     *
     * @param code
     */
    private void uploadFailed(int code) {
        NotificationCompat.Builder foregroundBuilder =
                new NotificationCompat.Builder(getApplicationContext()).
                        setContentTitle("DOSEPICS").
                        setContentText("FAILED: " + code + " " + RestService.responseCodeHumanForm(code)).
                        setSmallIcon(R.mipmap.ic_launcher);
        Notification errorNot = foregroundBuilder.build();
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTE_ID_ERROR, errorNot);
        stopSelf();
    }

    /**
     * Main entry point!
     *
     * @param intent    - Intent that contains information about how to proceed
     * @param flags     - not used
     * @param startId   - not used
     * @return          - Irrelevant, the Service does not handle restarts
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_UPLOAD.equals(action)) {
                description = intent.getStringExtra(EXTRA_DESCRIPTION);
                owner = intent.getStringExtra(EXTRA_OWNER);
                handleActionUpload();
            }
        }
        return START_NOT_STICKY;
    }

    /**
     * Not used
     *
     * @param intent    - not used
     * @return          - null to indicate that binding is not supported
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Begin a new upload
     */
    private void handleActionUpload() {

        // Calculate the number of chunks to be uploaded
        int len = ImageDataHolder.getData().length;
        chunks = len / CHUNK_SIZE + 1;

        // Create progress bar to inform user that the image is uploading.
        // Make it indeterminate until the first chunk has actually been
        // successfully uploaded
        foregroundBuilder =
                new NotificationCompat.Builder(getApplicationContext()).
                        setProgress(0, 100, true).
                        setContentTitle("DOSEPICS").
                        setContentText("uploading...").
                        setSmallIcon(R.mipmap.ic_launcher);
        foregroundNot = foregroundBuilder.build();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        currentChunk = 1;
        startForeground(NOTE_ID_PROGRESS, foregroundNot);

        BackgroundUpload bu = new BackgroundUpload();
        new Thread(bu).start();
    }

    /**
     * Handles the upload of a chunk
     */
    private void uploadChunk() {

        // The first chunk is a little special: besides image data it also
        // contains the owner of and information about the picture, along with
        // the total number of chunks
        if (currentChunk == 1) {
            networkState = NetworkState.NETWORK_WAIT_FOR_OK;
            try {
                // Put initial info to the body
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("chunks", chunks);
                jsonBody.put("owner", owner);
                jsonBody.put("info", description);

                // Get partial image data into body holder
                int noBytes = CHUNK_SIZE > ImageDataHolder.getData().length ?
                        ImageDataHolder.getData().length : CHUNK_SIZE;
                ByteBuffer bb = ByteBuffer.allocate(noBytes);
                bb.put(ImageDataHolder.getData(), 0, noBytes);
                String jsonChunk = Base64.encodeToString(bb.array(), Base64.DEFAULT);
                jsonBody.put("image", jsonChunk);

                // Store for RestService
                BodyHolder.setData("");
                BodyHolder.setData(jsonBody.toString());

                // First POST requires authorization
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String resource = sp.getString("resource", "");
                String loginUsername = sp.getString("username", "");
                String loginPassword = sp.getString("password", "");
                RestService.startActionPost(getApplicationContext(), resource + "/pics", loginUsername, loginPassword);
            } catch (Exception e) {
                Log.d("DOSESE", "Exception! " + e.toString());
                uploadFailed(-1);
            }
        } else {
            // Normal (non-first) chunk
            try {

                // Get partial image data into body holder
                JSONObject jsonBody = new JSONObject();

                int chunkSize = CHUNK_SIZE * (currentChunk) > ImageDataHolder.getData().length ?
                        ImageDataHolder.getData().length - CHUNK_SIZE * (currentChunk - 1):
                        CHUNK_SIZE;

                byte[] chunk = new byte[chunkSize];
                ByteBuffer bb = ByteBuffer.allocate(chunkSize);
                bb.put(ImageDataHolder.getData(), (currentChunk - 1) * CHUNK_SIZE, chunkSize);
                String jsonChunk = Base64.encodeToString(bb.array(), Base64.DEFAULT);
                jsonBody.put("image", jsonChunk);

                // Store for RestService
                BodyHolder.setData("");
                BodyHolder.setData(jsonBody.toString());

                // Non-first POST requests ignore authorization. Only the cookie
                // is needed
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String resource = sp.getString("resource", "");

                RestService.startActionPost(getApplicationContext(), resource + "/pics", cookie);

            } catch (Exception e) {
                Log.d("DOSESE", "Exception! " + e.toString());
                uploadFailed(-1);
            }
        }

        // Was it the last chunk?
        if(currentChunk == (chunks))
        {
            networkState = NetworkState.NETWORK_WAIT_FOR_CREATED;
        }
    }

    /**
     * Handles a response code. If we have more chunks to upload, we wait for
     * 200 (OK) and if we are done we wait for a 201 (Created). Anything else
     * is a failure
     *
     * @param intent
     */
    private void handleResponseCode(Intent intent)
    {
        int responseCode = intent.getIntExtra(RestService.REST_RESPONSE_CODE, 0);
        //Log.d("DOSESE", "UploadImageService BroadcastReceiver got response code:" + responseCode);

        if(responseCode == HTTP_OK && networkState == NetworkState.NETWORK_WAIT_FOR_OK)
        {
            // One chunk has been successfully uploaded; process the next one
            currentChunk++;

            // Update notification in progress bar
            int progress = (int) ((100.0 / chunks) * (1.0 * currentChunk));
            foregroundBuilder.setProgress(100, progress, false);
            notificationManager.notify(NOTE_ID_PROGRESS, foregroundBuilder.build());

            // Upload next chunk
            BackgroundUpload bu = new BackgroundUpload();
            new Thread(bu).start();

        } else if (responseCode == HTTP_CREATED && networkState == NetworkState.NETWORK_WAIT_FOR_CREATED)
        {
            // Upload complete
            networkState = NetworkState.NETWORK_IDLE;
            createDoneNotification();
            stopSelf();
        } else
        {
            Toast.makeText(this, "Illegal response code: " + responseCode, Toast.LENGTH_SHORT).show();
            Log.d("DOSESE", "Illegal response code " + responseCode);
            uploadFailed(responseCode);
        }
    }

    /**
     * Cleanup before the Service is destroyed. We MUST unregister the RestService
     * listener, or else the communication gets messed up the next time the Service
     * is restarted.
     */
    @Override
    public void onDestroy()
    {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(restReceiver);
    }

    /**
     * Handles a cookie from RestService.
     *
     * Just saves it for future use
     *
     * @param intent
     */
    private void handleCookie(Intent intent)
    {
        cookie = intent.getStringExtra(RestService.REST_RESPONSE_COOKIE);
        //Log.d("DOSESE", "Cookie received: " + cookie);
    }

    /**
     * A body has been received. This is not used
     *
     * @param intent
     */
    private void handleResponseBody(Intent intent)
    {
        String body = intent.getStringExtra(RestService.REST_RESPONSE_BODY);
        //Log.d("DOSESE", "Body received: " + body);
    }

    /**
     * Simple class that deals with the upload in the background
     */
    public class BackgroundUpload implements Runnable {

        public void run()
        {
            uploadChunk();
        }
    }
}
