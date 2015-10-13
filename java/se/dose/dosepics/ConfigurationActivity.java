package se.dose.dosepics;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class ConfigurationActivity extends AppCompatActivity  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleNormalStart();
    }

    private void handleNormalStart()
    {
        setContentView(R.layout.activity_configuration);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        if(null != getSupportActionBar())
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

        editor.apply();

        Toast.makeText(getApplicationContext(), "Changes applied!",Toast.LENGTH_SHORT).show();

        // Does this work?
        Intent i = new Intent(this, ImageActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
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
        cb.setChecked(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_configuration, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_user_administration) {
            Intent i = new Intent(this, AdminUsersActivity.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }

}