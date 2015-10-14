package se.dose.dosepics;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A dialog shown in AdminUsersActivity to give the user the option to update
 * hers or, if she has administrative rights, others users' information.
 */
public class UserInfoFragment extends DialogFragment {

    // Info to be shown about the user
    private String username = "";
    private String realName = "";
    private boolean requestorIsAdmin = false;

    /* Info about the user asking for information to be shown
     * This is needed in order to provide the user with applicable buttons
     * I.e., no need to show a "delete" button if the user has no rights to
     * delete any user, etc. */
    private String requestorUser = "";
    private boolean isAdmin = false;

    // Information to be sent back to caller in case the user decides to update information
    private String newPassword = "";
    private String newName = "";
    private boolean newAdminCheck = false;

    private UserInfoFragmentListener listener;


    /**
     * Create a Dialog with custom layout
     *
     * @param savedInstanceState - not used
     * @return                  Created dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // If we can't alter the user's info, an "Apply" button is meaningless
        if(requestorUser.equals(username) || requestorIsAdmin) {
            builder.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    // Get info to be sent back to requestor
                    EditText et = (EditText) getDialog().findViewById(R.id.input_password);
                    newPassword = et.getText().toString();
                    et = (EditText) getDialog().findViewById(R.id.input_real_name);
                    newName = et.getText().toString();
                    CheckBox adminBox = (CheckBox) getDialog().findViewById(R.id.check_admin);
                    newAdminCheck = adminBox.isChecked();

                    // Ask the user to confirm the update of her own password
                    if(requestorUser.equals(username))
                    {
                        AlertDialog.Builder yesNoBuilder = new AlertDialog.Builder(getActivity());
                        yesNoBuilder.setTitle("Really update your own info, including password?");
                        yesNoBuilder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                listener.onUserInfoUpdated(username, newName, newPassword, newAdminCheck);
                            }
                        });
                        yesNoBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing
                            }
                        });
                        yesNoBuilder.show();

                    } else
                        listener.onUserInfoUpdated(username, newName, newPassword, newAdminCheck);
                }
            });
        }

        // Kinda pointless do have a delete button if we can't use it, right?
        if(requestorIsAdmin) {
            builder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    // Create new AlertDialog to ask user to confirm this action
                    AlertDialog.Builder yesNoBuilder = new AlertDialog.Builder(getActivity());
                    yesNoBuilder.setTitle("Really delete " + username + "?");
                    yesNoBuilder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            listener.onUserDeleted(username);
                        }
                    });
                    yesNoBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                        }
                    });
                    yesNoBuilder.show();
                }
            });
        }

        // For clarity's sake we call a listener that does nothing if the
        // user clicks cancel
        builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onUserInfoCancelClick();
            }
        });

        // Create the custom Dialog
        builder.setTitle("About " + username);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_user_info, null);
        builder.setView(view);

        TextView textViewRealName = (TextView) view.findViewById(R.id.input_real_name);
        textViewRealName.setText(realName);

        CheckBox adminBox = (CheckBox) view.findViewById(R.id.check_admin);
        adminBox.setChecked(isAdmin);

        TextView textViewPassword = (TextView) view.findViewById(R.id.input_password);
        if(requestorIsAdmin)
        {
            adminBox.setEnabled(true);
            textViewRealName.setEnabled(true);
            textViewPassword.setEnabled(true);
        } else if(requestorUser.equals(username))
        {
            adminBox.setEnabled(false);
            textViewRealName.setEnabled(true);
            textViewPassword.setEnabled(true);
        } else {
            adminBox.setEnabled(false);
            textViewRealName.setEnabled(false);
            textViewPassword.setEnabled(false);
        }
        return builder.create();
    }

    /**
     * Hook up listener
     * @param activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (UserInfoFragmentListener) activity;
    }

    /*
     * Below are some setters that need to be called after creating and before
     * showing UserInforFragment Dialog
     */
    public void setRealName(String realName) {
        this.realName = realName;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    public void setRequestorUser(String requestorUser) {
        this.requestorUser = requestorUser;
    }

    public void setRequestorIsAdmin(boolean requestorIsAdmin) {
        this.requestorIsAdmin = requestorIsAdmin;
    }

    // Listener interface
    public interface UserInfoFragmentListener {
        void onUserInfoUpdated(String userName, String realName, String password, boolean isAdmin);
        void onUserDeleted(String userName);
        void onUserInfoCancelClick();
    }
}
