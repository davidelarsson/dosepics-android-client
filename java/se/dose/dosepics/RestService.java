package se.dose.dosepics;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class RestService extends IntentService {

    public static final String ACTION_DELETE = "se.dose.dosepics.action.DELETE";
    public static final String ACTION_GET = "se.dose.dosepics.action.GET";
    public static final String ACTION_POST = "se.dose.dosepics.action.POST";
    public static final String ACTION_PUT = "se.dose.dosepics.action.PUT";

    public static final String EXTRA_RESOURCE = "se.dose.dosepics.extra.RESOURCE";
    public static final String EXTRA_USERNAME = "se.dose.dosepics.extra.USERNAME";
    public static final String EXTRA_PASSWORD = "se.dose.dosepics.extra.PASSWORD";
    public static final String EXTRA_COOKIE = "se.dose.dosepics.extra.COOKIE";

    public static final String INTENT_FILTER = "REST-event";
    public static final String REST_RESPONSE_BODY = "REST-response-body";
    public static final String REST_RESPONSE_CODE = "REST-response-code";
    public static final String REST_RESPONSE_COOKIE = "REST-response-cookiee";

    /*
     * A constructor is mandatory
     */
    public RestService() {
        super("RestService");
    }

    /*************************************************************************************
     * Below is a list of static help functions
     *************************************************************************************/

    /*
     * Static help function to start a POST action with username and password
     */
    public static void startActionPost(Context context, String resource, String username, String password) {
        Intent intent = new Intent(context, RestService.class);

        intent.putExtra(EXTRA_RESOURCE, resource);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_PASSWORD, password);

        intent.setAction(ACTION_POST);
        context.startService(intent);
    }

    /*
     * Static help function to start a POST action with a cookie
     */
    public static void startActionPost(Context context, String resource, String cookie)
    {
        Intent intent = new Intent(context, RestService.class);

        intent.putExtra(EXTRA_RESOURCE, resource);
        intent.putExtra(EXTRA_COOKIE, cookie);

        intent.setAction(ACTION_POST);
        context.startService(intent);
    }

    /*
     * Static help function to start a DELETE action
     */
    public static void startActionDelete(Context context, String resource, String username, String password)
    {
        Intent intent = new Intent(context, RestService.class);

        intent.putExtra(EXTRA_RESOURCE, resource);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_PASSWORD, password);

        intent.setAction(ACTION_DELETE);
        context.startService(intent);
    }

    /*
     * Static help function to start a GET action
     */
    public static void startActionGet(Context context, String resource)
    {
        Intent i = new Intent(context, RestService.class);
        i.setAction(ACTION_GET);
        i.putExtra(EXTRA_RESOURCE, resource);
        context.startService(i);
    }

    /*
     * Static help function to start a PUT action
     */
    public static void startActionPut(Context context, String resource, String username, String password)
    {
        Intent i = new Intent(context, RestService.class);
        i.setAction(ACTION_PUT);
        i.putExtra(EXTRA_USERNAME, username);
        i.putExtra(EXTRA_PASSWORD, password);
        i.putExtra(EXTRA_RESOURCE, resource);
        context.startService(i);
    }

    /*************************************************************************************
     * End of static help functions
     ************************************************************************************/

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_POST.equals(action)) {
                handleActionPost(intent);
            } else if (ACTION_GET.equals(action)) {
                handleActionGet(intent);
            } else if(ACTION_DELETE.equals(action)) {
                handleActionDelete(intent);
            } else if(ACTION_PUT.equals(action)) {
                handleActionPut(intent);
            }
        } else
        {
            // No action has been supplied
        }
    }

    private void handleActionGet(Intent intent)
    {
        String resource = intent.getStringExtra(EXTRA_RESOURCE);
        URL url;
        HttpURLConnection conn;

        // Body is ignored when using GET

        try {
            url = new URL(resource);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Request body is ignored when using GET

            // Response must be read, otherwise nothing happens
            String responseBody = null;
            int responseCode = conn.getResponseCode();
            if(responseCode >= 400)
                responseBody = readStream(conn.getErrorStream());
            else
                responseBody = readStream(conn.getInputStream());

            // Deliver back to questioner
            deliverResponseCode(responseCode);
            if(!responseBody.equals(""))
                deliverBody(responseBody);

        } catch (Exception e)
        {
            Log.d("DOSESE", "handleActionGet EXCEPTION: " + e.toString());
        }
    }

    private void handleActionDelete(Intent intent)
    {
        String resource = intent.getStringExtra(EXTRA_RESOURCE);
        String loginUsername = intent.getStringExtra(EXTRA_USERNAME);
        String loginPassword = intent.getStringExtra(EXTRA_PASSWORD);
        URL url;
        HttpURLConnection conn;

        try {
            url = new URL(resource);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");

            // Create authentication header
            byte[] bytes = (loginUsername + ":" + loginPassword).getBytes();
            String hash = Base64.encodeToString(bytes, Base64.DEFAULT);
            conn.addRequestProperty("Authorization", "Basic " + hash);

            // Request body is ignored when using DELETE

            // Response must be read, otherwise nothing happens
            String responseBody = null;
            int responseCode = conn.getResponseCode();
            deliverResponseCode(responseCode);
            if(responseCode >= 400)
                responseBody = readStream(conn.getErrorStream());
            else
                responseBody = readStream(conn.getInputStream());

        } catch (Exception e)
        {
        }
    }

    private void handleActionPut(Intent intent)
    {
        String resource = intent.getStringExtra(EXTRA_RESOURCE);
        String loginUsername = intent.getStringExtra(EXTRA_USERNAME);
        String loginPassword = intent.getStringExtra(EXTRA_PASSWORD);
        URL url;
        HttpURLConnection conn;

        try {
            url = new URL(resource);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");

            // Create authentication header
            byte[] bytes = (loginUsername + ":" + loginPassword).getBytes();
            String hash = Base64.encodeToString(bytes, Base64.DEFAULT);
            conn.addRequestProperty("Authorization", "Basic " + hash);

            OutputStream os = conn.getOutputStream();
            byte[] jsonBytes = BodyHolder.getData().getBytes();
            os.write(jsonBytes);
            os.close();

            // Response must be read, otherwise nothing happens
            String responseBody = null;
            int responseCode = conn.getResponseCode();
            deliverResponseCode(responseCode);
            if(responseCode >= 400)
                responseBody = readStream(conn.getErrorStream());
            else
                responseBody = readStream(conn.getInputStream());

        } catch (Exception e)
        {
        }
    }

    private void handleActionPost(Intent intent)
    {
        String resource = intent.getStringExtra(EXTRA_RESOURCE);
        String loginUsername = intent.getStringExtra(EXTRA_USERNAME);
        String loginPassword = intent.getStringExtra(EXTRA_PASSWORD);
        String cookie = "";
        if(intent.hasExtra(EXTRA_COOKIE))
            cookie = intent.getStringExtra(EXTRA_COOKIE);
        URL url;
        HttpURLConnection conn;

        try {
            url = new URL(resource);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true); // Required when doing a POST
            conn.setRequestMethod("POST");

            // In case we have a cookie, supply this
            if(!cookie.equals("")) {
                conn.setRequestProperty("Cookie", cookie);
            }

            // Create authentication header
            byte[] bytes = (loginUsername + ":" + loginPassword).getBytes();
            String hash = Base64.encodeToString(bytes, Base64.DEFAULT);
            conn.addRequestProperty("Authorization", "Basic " + hash);

            // Write body
            OutputStream os = conn.getOutputStream();
            byte[] jsonBytes = BodyHolder.getData().getBytes();
            os.write(jsonBytes);
            os.close();

            // Response must be read, otherwise nothing happens
            String responseBody = null;
            int responseCode = conn.getResponseCode();
            deliverResponseCode(responseCode);
            if(responseCode >= 400)
                responseBody = readStream(conn.getErrorStream());
            else
                responseBody = readStream(conn.getInputStream());
            if(!responseBody.equals(""))
                deliverBody(responseBody);

            // Get response cookie
            Map<String, List<String>> headers = conn.getHeaderFields();
            List<String> cookies = headers.get("Set-Cookie");
            if(cookies != null)
            {
                cookie = cookies.get(0);
                if(cookie != null && cookie != "") {
                    deliverResponseCookie(cookie);
                }
            }

        } catch (Exception e) {
            deliverBody("Exception while POSTing: " + e.toString());
            deliverResponseCode(-1);
        }
    }

    private String readStream(InputStream is)
    {
        BufferedReader r;
        StringBuilder res = new StringBuilder();
        try {
            r = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = r.readLine()) != null)
            {
                res.append(line);
            }
        } catch (Exception e)
        {
        }
        return res.toString();
    }

    private void deliverBody(String body)
    {
        Intent i = new Intent(INTENT_FILTER);
        i.putExtra(REST_RESPONSE_BODY, body);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    private void deliverResponseCode(int code)
    {
        Intent i = new Intent(INTENT_FILTER);
        i.putExtra(REST_RESPONSE_CODE, code);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    private void deliverResponseCookie(String cookie)
    {
        Intent i = new Intent(INTENT_FILTER);
        i.putExtra(REST_RESPONSE_COOKIE, cookie);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    public static String responseCodeHumanForm(int code)
    {
        switch(code) {
            case 400:
                return "Bad Request";
            case 403:
                return "Forbidden";
            case 404:
                return "Not Found";
            case 405:
                return "Method Not Allowed";
            case 409:
                return "Conflict";
            case 422:
                return "Unprocessable Entity";
            case 500:
                return "Internal Server Error";
            case -1:
                return "RestService error";
            default:
                return "Unknown Error";
        }
    }
}
