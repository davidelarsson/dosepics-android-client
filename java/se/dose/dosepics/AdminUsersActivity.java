package se.dose.dosepics;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An activity that handles the administration of users on the server
 */
public class AdminUsersActivity extends AppCompatActivity implements
        NewUserFragment.NewUserFragmentListener,
        UserInfoFragment.UserInfoFragmentListener,
        SwipeRefreshLayout.OnRefreshListener 	{

    // FIXME: Change this!
    // Info about a user to be updated
    Map<String, String> userInfoToBeUpdated = null;

    // Keep track of whether we have the administrator flag set for ourselves
    private boolean weAreAdmin = false;

    // The URL of the base API
    private String resource;

    // Response code from RestService
    private int responseCode = 0;

    // Response responseBody from RestService
    private String responseBody;

    // FIXME: Not sure this is the best way to handle a progress dialog.
    // FIXME: Maybe instantiate it more often instead?
    private ProgressDialog progressDialog = null;

    // Keep track of what response we're waiting for from RestService
    private enum NetworkState {
        IDLE,
        WAIT_GET_USERS,
        WAIT_NEW_USER,
        WAIT_DELETE_USER,
        WAIT_UPDATE_USER_INFO,
        WAIT_USER_INFO,
        WAIT_AM_I_ADMIN };
    private NetworkState networkState = NetworkState.IDLE;

    /**
     * Called when the activity is starting. Main entry point of the Activity
     *
     * @param savedInstanceState - not used
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        // Listener for Swipe Refresh
        SwipeRefreshLayout srl = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        srl.setOnRefreshListener(this);

        // Listener for the RestService
        LocalBroadcastManager.getInstance(this).registerReceiver(restReceiver,
                new IntentFilter(RestService.INTENT_FILTER));

        // Set the resource
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        resource = sp.getString("resource", "");

        // Start by filling the list with users
        weAreAdmin = false;
        checkAmIAdmin();
    }

    /**
     * Cleanup before the activity is destroyed. We MUST unregister the RestService
     * listener, or else the communication gets messed up the next time the Activity
     * is restarted.
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d("DOSESE", "UploadImageService destroyed");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(restReceiver);
    }

    /**
     * Call RestService to get a list of available users
     */
    private void updateUserList() {
        networkState = NetworkState.WAIT_GET_USERS;
        setProgressBar("Getting users...");
        RestService.startActionGet(this, resource + "/users");
    }

    /**
     * RestService uses broadcasts to send data back to the inquirer
     */
    private BroadcastReceiver restReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(RestService.REST_RESPONSE_BODY)) {
                responseBody = intent.getStringExtra(RestService.REST_RESPONSE_BODY);
                handleReceivedBody();
            } else if (intent.hasExtra(RestService.REST_RESPONSE_CODE)) {
                responseCode = intent.getIntExtra(RestService.REST_RESPONSE_CODE, 0);
                handleReceivedResultCode();
            }
        }
    };

    /**
     * Called when the user does a swipe to refresh the users list
     */
    @Override
    public void onRefresh()
    {
        updateUserList();
    }

    /**
     * RestService has sent us a body. Interpret it!
     */
    private void handleReceivedBody() {
        //Log.d("DOSESE", "handleReceivedBody(); " + responseBody);

        if (networkState == NetworkState.WAIT_GET_USERS) {
            handleUsersReceived();
        } else if (networkState == NetworkState.WAIT_NEW_USER) {
            // We don't want a responseBody when we have just created a new user
            Toast.makeText(this, "INTERNAL ERROR: Body received when created user", Toast.LENGTH_SHORT).show();
            networkState = NetworkState.IDLE;
            setProgressBar("");
        } else if (networkState == NetworkState.WAIT_USER_INFO) {
            handleUserInfoReceived();
        } else if(networkState == NetworkState.WAIT_AM_I_ADMIN) {
            handleUserAdminChecked();
        }

        // Whatever we have received, we are not waiting for a refresh to complete.
        // Yeah, this ought to be inside handleUsersReceived(). But sometimes,
        // if the user is quick to click an item in the list while the list is being
        // updated, the responses from RestService get a little mixed up.Just do a
        // new refresh swipe!
        //
        // A more elegant solution would be to rewrite RestService to extend AsyncTask
        SwipeRefreshLayout srl = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        srl.setRefreshing(false);
    }

    /**
     * RestService has sent us a response code. Interpret it!
     */
    private void handleReceivedResultCode() {
        if (networkState == NetworkState.WAIT_NEW_USER) {
            if (responseCode == 201) {
                Toast.makeText(this, "User created", Toast.LENGTH_SHORT).show();

                // A new user has been created, update the user list
                updateUserList();
            } else {
                Toast.makeText(this, "ERROR creating user: " + responseCode + " " + RestService.responseCodeHumanForm(responseCode), Toast.LENGTH_SHORT).show();
                networkState = NetworkState.IDLE;
                setProgressBar("");
            }
        } else if (networkState == NetworkState.WAIT_DELETE_USER) {
            if (responseCode == 200) {
                Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();

                // A user has been deleted, update the user list
                updateUserList();
            } else {
                Toast.makeText(this, "ERROR deleting user: " + responseCode + " " + RestService.responseCodeHumanForm(responseCode), Toast.LENGTH_SHORT).show();
                networkState = NetworkState.IDLE;
                setProgressBar("");
            }
        } else if (networkState == NetworkState.WAIT_UPDATE_USER_INFO) {
            if (responseCode == 200) {
                updateUserInfoItem();
                Toast.makeText(this, "User info updated", Toast.LENGTH_SHORT).show();
            } else {
                setProgressBar("");
                networkState = NetworkState.IDLE;
                Toast.makeText(this, "ERROR updating user info: " + responseCode + " " + RestService.responseCodeHumanForm(responseCode), Toast.LENGTH_SHORT).show();
            }
        } else if(networkState == NetworkState.WAIT_AM_I_ADMIN)
        {
            if(responseCode >= 400) {
                // We could not find out whether we have administrative privileges
                networkState = NetworkState.IDLE;
                showYouAreNotADminDialog();
                updateUserList();
            }
        }
    }

    /**
     * Check whether the user has administrative privileges, and set the field
     * variable accordingly
     */
    private void checkAmIAdmin()
    {
        networkState = NetworkState.WAIT_AM_I_ADMIN;

        // Get the user's username
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String user = sp.getString("username", "");

        // Get information about ourselves
        RestService.startActionGet(getApplicationContext(), resource + "/users/" + user);
    }

    /**
     * Creating a dialog box telling the user of her restrictions because she
     * does not have the administrator flag set
     */
    private void handleUserAdminChecked()
    {
        weAreAdmin = false;
        try{
            JSONObject jsonInfo = new JSONObject(responseBody);
            weAreAdmin = jsonInfo.getBoolean("admin");
        } catch (Exception e)
        {
            Log.d("DOSESE", "handleUserAdminChecked EXCEPTION: " + e.toString());
            // Without this the app crashes sometimes when starting the alert dialog
            return;
        } finally
        {
            networkState = NetworkState.IDLE;
            updateUserList();
        }
        if(!weAreAdmin)
            showYouAreNotADminDialog();

    }

    /**
     * Silly help function to show a dialog telling the user she does not
     * have administrative rights
     */
    private void showYouAreNotADminDialog()
    {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("NOTE!");
        adb.setPositiveButton("Got it!", null);
        adb.setMessage("You do NOT have administrative rights, and as such you can not " +
                "modify any user information but your own!");
        adb.create().show();
    }

    /**
     * Information about a user has been received from RestService. Handle it
     * by showing a dialog with a layout from UserInfoFragment
     */
    private void handleUserInfoReceived()
    {
        String username = "";
        String realName = "";
        boolean isAdmin = false;
        try{
            JSONObject jsonInfo = new JSONObject(responseBody);
            username = jsonInfo.getString("username");
            realName = jsonInfo.getString("name");
            isAdmin= jsonInfo.getBoolean("admin");
        }catch (Exception e)
        {
            Toast.makeText(this, "Error in user list: " + e.toString(), Toast.LENGTH_SHORT).show();
            //YAY
            return;
        } finally
        {
            networkState = NetworkState.IDLE;
            setProgressBar("");
        }

        UserInfoFragment uif = new UserInfoFragment();
        uif.setUsername(username);
        uif.setRealName(realName);
        uif.setIsAdmin(isAdmin);
        uif.setRequestorIsAdmin(weAreAdmin);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        uif.setRequestorUser(sp.getString("username", ""));
        uif.show(getFragmentManager(), "UserInfoFragment");
    }

    /**
     * A list of users has been received from RestService.
     * Handle it by updating the list with this new information
     */
    private void handleUsersReceived() {
        //Log.d("DOSESE", "handleUsersReceived()");
        ListView lv = (ListView) findViewById(android.R.id.list);

        try {
            JSONArray jsonUsers = new JSONArray(responseBody);
            ArrayList<String> userList = new ArrayList<String>();

            for (int i = 0; i < jsonUsers.length(); i++) {
                userList.add(jsonUsers.getString(i));
            }

            String[] userStrings = userList.toArray(new String[userList.size()]);
            //ListView lv = (ListView) findViewById(android.R.id.list);
            lv.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, userStrings));

            // If item is clicked, start UserInfoFragment with more information
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                    ListView lv = (ListView) parent;
                    String user = lv.getItemAtPosition(pos).toString();

                    // Get info about user
                    networkState = NetworkState.WAIT_USER_INFO;
                    setProgressBar("Getting user info...");
                    RestService.startActionGet(getApplicationContext(), resource + "/users/" + user);
                }
            });
        } catch (Exception e) {
            Log.d("DOSESE", "ERROR: Could not create user list! " + e.toString());
            Toast.makeText(this, "ERROR: Could not create user list! " + e.toString(), Toast.LENGTH_LONG).show();
        } finally {
            networkState = NetworkState.IDLE;
            setProgressBar("");
        }
    }

    /**
     * User has decided to create a new user in UserInfoFragment. Do so by
     * sending a request to RestService about this
     */
    public void onNewUserPositiveClick(String username, String name, String password, boolean isAdmin) {

        if(username.equals(""))
            return;

        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("name", name);
            body.put("pwd", password);
            body.put("admin", isAdmin);
            BodyHolder.setData("");
            BodyHolder.setData(body.toString());

            // Authenticate ourselves
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String loginUsername = sp.getString("username", "");
            String loginPassword = sp.getString("password", "");

            RestService.startActionPost(this, resource + "/users", loginUsername, loginPassword);
            networkState = NetworkState.WAIT_NEW_USER;
            setProgressBar("Creating user...");

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Could not create user: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * User has canceled the New User dialog
     *
     * Do nothing.
     */
    public void onNewUSerNegativeClick() {
        //Toast.makeText(this, "No user created", Toast.LENGTH_SHORT).show();
    }

    /**
     * Help function to create and remove the progress bar
     *
     * @param msg - if an empty string (""), disable the progress bar.
     *            - if contains a string, enable the progress with this string
     */
    private void setProgressBar(String msg)
    {
        if(progressDialog != null)
            progressDialog.cancel();

        if(!msg.equals(""))
            progressDialog = ProgressDialog.show(this, "", msg, true);
    }

    /**
     * User has decided to delete a new user in UserInfoFragment. Do so by
     * sending a request to RestService about this
     */
    public void onUserDeleted(String user) {
        networkState = NetworkState.WAIT_DELETE_USER;
        setProgressBar("Deleting user...");

        // Authenticate ourselves
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String loginUsername = sp.getString("username", "");
        String loginPassword = sp.getString("password", "");

        RestService.startActionDelete(this, resource + "/users/" + user, loginUsername, loginPassword);
    }

    /**
     * User has decided to update the information about a user
     *
     * FIXME: This is a little silly ought to be rewritten using queues instead
     *
     * The update of user items has to be done in steps, with one item at the
     * time. At least for non-admin users, since if one were to update the
     * entire user item at once, that would include the administrator flag,
     * which for natural reasons the user herself is not allowed to update.
     *
     * Hence, we divide the update into several calls. The silly solution I use
     * is to keep an array of fields to be updated together with three fields
     * that are used for the actual call each time. These reserved fields are
     * to be found in the array itemsToSkip in updateUserInfoItem.
     *
     * So yeah, queues would look a ton nicer.
     */
    public void onUserInfoUpdated(String user, String realName, String password, boolean isAdmin) {
        try {
            JSONObject body = new JSONObject();
            body.put("name", realName);
            body.put("pwd", password);
            body.put("admin", isAdmin);
            BodyHolder.setData("");
            BodyHolder.setData(body.toString());

            networkState = NetworkState.WAIT_UPDATE_USER_INFO;

            // Authenticate ourselves
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String loginUsername = sp.getString("username", "");
            String loginPassword = sp.getString("password", "");

            setProgressBar("Updating user info...");

            userInfoToBeUpdated = new HashMap<String, String>();
            userInfoToBeUpdated.put("loginUsername", loginUsername);
            userInfoToBeUpdated.put("loginPassword", loginPassword);
            userInfoToBeUpdated.put("user", user);
            userInfoToBeUpdated.put("name", realName);
            userInfoToBeUpdated.put("pwd", password);

            // The admin flag can only be updated if we are admin outselves
            if(weAreAdmin)
                userInfoToBeUpdated.put("admin", isAdmin ? "true" : "false");

            updateUserInfoItem();

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Could not update user info: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * FIXME: Rewrite this
     */
    private void updateUserInfoItem()
    {
        // Fields to skip
        String[] itemsToSkip = { "loginUsername", "loginPassword", "user" };

        // The map always contains "loginUsername", "loginPassword" and "user
        // If these are the only in left, we have nothing left to do
        if(userInfoToBeUpdated.size() <= itemsToSkip.length)
        {
            networkState = NetworkState.IDLE;
            setProgressBar("");
            Toast.makeText(getApplicationContext(), "User info updated!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get next item to update, but skip the skippable items
        Iterator iterator = userInfoToBeUpdated.entrySet().iterator();
        Map.Entry entry = (Map.Entry) iterator.next();
        for(int i = 0; i < itemsToSkip.length; i++) {
            if (entry.getKey().equals(itemsToSkip[i])) {
                entry = (Map.Entry) iterator.next();
            }
        }

        // Get info needed for the REST call
        String loginUsername = userInfoToBeUpdated.get("loginUsername");
        String loginPassword = userInfoToBeUpdated.get("loginPassword");
        String user = userInfoToBeUpdated.get("user");

        String resource = this.resource + "/users/" + user + "/" + entry.getKey();
        RestService.startActionPut(this, resource, loginUsername, loginPassword);
        iterator.remove();
    }

    /**
     * User has canceled the UserInfoDialog
     *
     * Do nothing
     */
    public void onUserInfoCancelClick()
    {
        // Do nothing
    }

    /**
     * Manual quote:
     * "Prepare the Screen's standard options menu to be displayed. This is
     * "called right before the menu is shown, every time it is shown. You
     * "can use this method to efficiently enable/disable items or otherwise
     * "dynamically modify the contents."
     *
     * We don't want the user to be able to press "New user" unless she has
     * administrative rights
     *
     * @param menu  The menu to be prepared
     * @return true
     */
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {

        if (weAreAdmin)
            menu.getItem(0).setEnabled(true);
        else
            menu.getItem(0).setEnabled(false);

        return true;
    }

    /**
     * Manual Quote:
     * "Initialize the contents of the Activity's standard options menu."
     *
     * We just supply the item(s) from the menu_admin_users XML file
     *
     * @param menu
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_admin_users, menu);
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_new_user) {
            NewUserFragment nuf = new NewUserFragment();
            nuf.show(getFragmentManager(), "GetDescriptionFragment");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
