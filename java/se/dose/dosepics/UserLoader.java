package se.dose.dosepics;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.MatrixCursor;

public class UserLoader extends CursorLoader {

    String received_data[] = {"madman1", "madman2"};

    public UserLoader(Context context)
    {
        super(context);
    }

    @Override
    public Cursor loadInBackground()
    {
        return new MatrixCursor(received_data);
    }
}
