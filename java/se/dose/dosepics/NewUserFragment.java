package se.dose.dosepics;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;

public class NewUserFragment extends DialogFragment {

    private NewUserFragmentListener listener;

    public NewUserFragment() {
        // Required empty public constructor
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setPositiveButton("Submit",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        EditText et = (EditText) getDialog().findViewById(R.id.input_username);
                        String username = et.getText().toString();

                        et = (EditText) getDialog().findViewById(R.id.input_name);
                        String name = et.getText().toString();

                        et = (EditText) getDialog().findViewById(R.id.input_password);
                        String password = et.getText().toString();

                        CheckBox adminBox = (CheckBox) getDialog().findViewById(R.id.new_admin);
                        boolean isAdmin = adminBox.isChecked();

                        listener.onNewUserPositiveClick(username, name, password, isAdmin);
                    }
                });
        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onNewUSerNegativeClick();
                    }
                });
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_new_user, null));
        builder.setTitle("Please enter information about the new user");
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (NewUserFragmentListener) activity;
    }

    public interface NewUserFragmentListener {
        void onNewUserPositiveClick(String username, String name, String password, boolean isAdmin);
        void onNewUSerNegativeClick();
    }
}
