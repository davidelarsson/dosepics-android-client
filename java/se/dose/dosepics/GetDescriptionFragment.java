package se.dose.dosepics;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

/**
 * An invisible activity that shows a dialog asking the user to desribe the
 * image just about to be shared
 */
public class GetDescriptionFragment extends DialogFragment {

    private EditText descriptionInput;
    GetDescriptionDialogListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        descriptionInput = new EditText(getActivity());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setPositiveButton("Submit",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onDialogPositiveClick(descriptionInput.getText().toString());
                    }
                });

        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onDialogNegativeClick();
                    }
                });

        builder.setTitle("Please enter a description:");
        builder.setView(descriptionInput);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    public interface GetDescriptionDialogListener {
        void onDialogPositiveClick(String description);
        void onDialogNegativeClick();
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        listener = (GetDescriptionDialogListener) activity;
    }
}
