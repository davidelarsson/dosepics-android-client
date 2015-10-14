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

/**
 * An activity that sets up the configuration of the application. Called when
 * the application is started the first time or when the user clicks "Configure
 * Dosepics" in ImageActivity
 */

public class ConfigurationActivity extends AppCompatActivity  {

    /**
     * Called when the activity is starting. Entry point for the Activity
     *
     * @param savedInstanceState - not used
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleNormalStart();
    }

    /**
     * Help function to start the Activity. Gets the saved state of the widgets
     * from SharedPreferences
     */
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

    /**
     * Callback for the "Apply" button in the GUI.
     *
     * Saves the state of all the GUI widgets and finishes the Activity
     *
     * @param v - not used
     */
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

        Intent i = new Intent(this, ImageActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    /**
     * Callback for the "Cancel" button in the GUI.
     *
     * Just finishes the Activity
     *
     * @param v - not used
     */
    public void onCancel(View v)
    {
        finish();
    }

    /**
     * Callback for the "Defaults" button in the GUI.
     *
     * Restores all the GUI widgets to their application-default state
     *
     * @param v - not used
     */
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

    /**
     * Manual quote:
     * "Prepare the Screen's standard options menu to be displayed. This is
     * "called right before the menu is shown, every time it is shown. You
     * "can use this method to efficiently enable/disable items or otherwise
     * "dynamically modify the contents."
     *
     * We don't want the user to be able to press the one and only options menu
     * item "User administration" if she has not yet entered a username. Before
     * this is done, we do not know what rights to give the user in
     * AdminUsersActivity
     *
     * @param menu  The menu to be prepared
     * @return true
     */
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        if (sp.contains("username"))
            menu.getItem(0).setEnabled(true);
        else
            menu.getItem(0).setEnabled(false);

        return true;
    }

    /**
     * Manual Quote:
     * "Initialize the contents of the Activity's standard options menu.
     * We just supply the item(s) from the menu_configuration XML file
     *
     * @param menu
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_configuration, menu);
        return true;
    }

    /**
     * Manual quote:
     * "This hook is called whenever an item in your options menu is selected."
     *
     * @param item
     * @return
     */
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