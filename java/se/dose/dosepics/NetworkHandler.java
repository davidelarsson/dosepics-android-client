package se.dose.dosepics;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

// Params, Progress, Result
// JSONObject, String, String
public class NetworkHandler extends AsyncTask<JSONObject, String, String> {

    private String api = "";
    private String owner = "";
    private String password = "";
    private String description = "";

    public Activity mainActivity = null;
    private String imageData = null;

    @Override
    protected void onPreExecute()
    {
  //      Toast.makeText(mainActivity.getApplicationContext(), "Uploading image with description \"" + description + "\"...", Toast.LENGTH_SHORT).show();
        Toast.makeText(mainActivity.getApplicationContext(), "Uploading image...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected String doInBackground(JSONObject... params)
    {
        URL url;
        HttpURLConnection conn;
        JSONArray response = new JSONArray();

        try {
            url = new URL(api);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            OutputStream os = conn.getOutputStream();
            JSONObject jsonData = new JSONObject();

            if(!owner.equals(""))
                jsonData.put("owner", owner);
            if(!password.equals(""))
                jsonData.put("password", password);
            if(!description.equals(""))
                jsonData.put("info", description);
            if(imageData != null)
                jsonData.put("image", imageData);

            String jsonString = jsonData.toString();
            byte[] jsonBytes = jsonString.getBytes();
            os.write(jsonBytes);
            os.close();

            // Without this nothing happens... FIXME: Figure out why!
            String responseString = readStream(conn.getInputStream());
        } catch (Exception e)
        {
            publishProgress("FAILED: Exception" + e.toString());
        }
        return response.toString();
    }

    @Override
    protected void onProgressUpdate(String... strings)
    {
        String str = strings[0];
        Toast.makeText(mainActivity.getApplicationContext(), str, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPostExecute(String result)
    {
        Toast.makeText(mainActivity.getApplicationContext(), "Dose pic uploaded!", Toast.LENGTH_SHORT).show();
    }

    private String readStream(InputStream is)
    {
        BufferedReader r = null;
        StringBuffer response = new StringBuffer();
        try {
            r = new BufferedReader(new InputStreamReader(is));
            String line = "";
            while ((line = r.readLine()) != null)
            {
                response.append(line);
            }
        } catch (Exception e)
        {

        }
        return response.toString();
    }

    public void setAPI(String api)
    {
        this.api = api;
    }
    public void setOwner(String owner)
    {
        this.owner = owner;
    }
    public void setPassword(String password)
    {
        this.password = password;
    }
    public void setImage(String imageData)
    {
        this.imageData = imageData;
    }
    public void setDescription(String description)
    {
    this.description = description;
    }

}

