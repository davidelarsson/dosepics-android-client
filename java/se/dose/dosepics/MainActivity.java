package se.dose.dosepics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity implements GetDescriptionFragment.GetDescriptionDialogListener {

    private BroadcastReceiver restReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String message = intent.getStringExtra("REST-result");
            Toast.makeText(getApplicationContext(), "REST result: " + message, Toast.LENGTH_SHORT).show();
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        String action = i.getAction();
        String type = i.getType();
        if(Intent.ACTION_SEND.equals(action) && type != null) {
            if(type.startsWith("image/"))
                handleShareImageStart();
        } else {
            handleNormalStart();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(restReceiver,
                new IntentFilter("REST-event"));
    }

    private void handleNormalStart()
    {
        setContentView(R.layout.activity_main);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            getSupportActionBar().setTitle("Dose Pics Configuration");

        if(sp.contains("resource"))
        {
            EditText et = (EditText) findViewById(R.id.resource);
            et.setText(sp.getString("resource", ""));
        }
        if(sp.contains("username"))
        {
            EditText et = (EditText) findViewById(R.id.username);
            et.setText(sp.getString("username", ""));
        }
        if(sp.contains("password"))
        {
            EditText et = (EditText) findViewById(R.id.password);
            et.setText(sp.getString("password", ""));
        }
        if(sp.contains("provide_description"))
        {
            CheckBox cb = (CheckBox) findViewById(R.id.provide_description);
            cb.setChecked(sp.getBoolean("provide_description", false));
        }
    }

    private void shareImage(String desc)
    {
        Uri imageUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(this);

        String resource = sp.getString("resource", "");
        String username = sp.getString("username", "");
        String password = sp.getString("password", "");

        try{
            JSONObject body = new JSONObject();
            body.put("owner", username);
            body.put("password", password);
            body.put("info", desc);

            // Convert shared image to JPEG
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageBytes = baos.toByteArray();
            String image_data = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            body.put("image", image_data);

            BodyHolder.setData(body.toString());
           // RestService.startActionPost(this, resource, "", "");

        } catch (Exception e)
        {
            Toast.makeText(getApplicationContext(), "Could not share image: " + e.toString(), Toast.LENGTH_SHORT).show();
        }
//        Toast.makeText(getApplicationContext(), "Uploading image...", Toast.LENGTH_SHORT).show();
    }

    private void handleShareImageStart()
    {
        Uri imageUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if(imageUri != null)
        {
            if(!sp.contains("resource")||
                !sp.contains("username")||
                !sp.contains("password")||
                !sp.contains("provide_description"))
            {
                Toast.makeText(getApplicationContext(), "Dose Pics is not configured!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        if(sp.getBoolean("provide_description", false))
            handle_get_description();
        else
            shareImage("");
    }

    private void handle_get_description()
    {
        GetDescriptionFragment gdf = new GetDescriptionFragment();
        gdf.show(getSupportFragmentManager(), "GetDescriptionFragment");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if(requestCode == 1 && resultCode == RESULT_OK)
        {
            String description = intent.getStringExtra("description");
            shareImage(description);
        }
        else {
            Toast.makeText(getApplicationContext(), "Not sharing anything...", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    public void onDialogPositiveClick(String description)
    {
        Toast.makeText(getApplicationContext(), "Sharing: " + description, Toast.LENGTH_SHORT).show();
        shareImage(description);
        finish();
    }

    public void onDialogNegativeClick()
    {
        Toast.makeText(getApplicationContext(), "Not sharing anything...", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void onApply(View v)
    {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();

        EditText et = (EditText) findViewById(R.id.resource);
        String resource = et.getText().toString();
        editor.putString("resource", resource);

        et = (EditText) findViewById(R.id.username);
        String username = et.getText().toString();
        editor.putString("username", username);

        et = (EditText) findViewById(R.id.password);
        String password = et.getText().toString();
        editor.putString("password", password);

        CheckBox cb = (CheckBox) findViewById(R.id.provide_description);
        editor.putBoolean("provide_description", cb.isChecked());

        editor.commit();

        Toast.makeText(getApplicationContext(), "Changes applied!",Toast.LENGTH_SHORT).show();
        finish();
    }

    public void onCancel(View v)
    {
        finish();
    }

    public void onDefaults(View v)
    {
        String default_resource = getString(R.string.default_resource);
        EditText et = (EditText) findViewById(R.id.resource);
        et.setText(default_resource);

        String default_username = getString(R.string.default_username);
        et = (EditText) findViewById(R.id.username);
        et.setText(default_username);

        String default_password = getString(R.string.default_password);
        et = (EditText) findViewById(R.id.password);
        et.setText(default_password);

        CheckBox cb = (CheckBox) findViewById(R.id.provide_description);
        cb.setChecked(false);
    }

    public void onAdminClicked(View v)
    {
        Intent i = new Intent(this, AdminUsersActivity.class);
        startActivity(i);
    }
}