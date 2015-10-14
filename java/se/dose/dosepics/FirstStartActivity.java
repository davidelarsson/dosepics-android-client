package se.dose.dosepics;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * A simple Activity that shows some information about how the Application is
 * meant to be used
 */
public class FirstStartActivity extends AppCompatActivity {
    private final String PREVIOUSLY_STARTED = "Previously started";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if(sp.getBoolean(PREVIOUSLY_STARTED, false)) {
            onStartApplicationClicked(null);
        }

        setContentView(R.layout.activity_first_start);
        final android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.hide();

    }

    public void onStartApplicationClicked(View v)
    {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();

        editor.putBoolean(PREVIOUSLY_STARTED, true);
        editor.commit();

        Intent i = new Intent(this, ImageActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

}
