package se.dose.dosepics;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

public class ShareActivity extends AppCompatActivity implements GetDescriptionFragment.GetDescriptionDialogListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        String action = i.getAction();
        String type = i.getType();
        if(Intent.ACTION_SEND.equals(action) && type != null) {
            if(type.startsWith("image/")) {
                handleShareImageStart();
            }
        }
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

    private void shareImage(String desc)
    {
        Uri imageUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(this);

        String username = sp.getString("username", "");

        try {
            // Convert shared image to JPEG
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

            // Save data for UploadImageService to read
            byte[] imageBytes = baos.toByteArray();
            ImageDataHolder.setData(imageBytes);
            UploadImageService.startActionUpload(this, username, desc);

        } catch (Exception e)
        {
            Toast.makeText(getApplicationContext(), "Could not share image: " + e.toString(), Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(getApplicationContext(), "Uploading image...", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void handle_get_description()
    {
        GetDescriptionFragment gdf = new GetDescriptionFragment();
        gdf.show(getSupportFragmentManager(), "GetDescriptionFragment");
    }

    // User has returned from GetDescriptionFragment dialog with a description
    public void onDialogPositiveClick(String description)
    {
        Toast.makeText(getApplicationContext(), "Sharing: " + description, Toast.LENGTH_SHORT).show();
        shareImage(description);
    }

    public void onDialogNegativeClick()
    {
        Toast.makeText(getApplicationContext(), "Not sharing anything...", Toast.LENGTH_SHORT).show();
        finish();
    }
}