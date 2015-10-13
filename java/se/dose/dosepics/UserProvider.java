package se.dose.dosepics;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class UserProvider extends ContentProvider {

    private String[] dummy_data = {"madman1", "madman2"};

    @Override
    public Cursor query (Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        return new MatrixCursor(dummy_data);
    }

    @Override
    public Uri insert (Uri uri, ContentValues values)
    {
        return null;
    }

    @Override
    public int update (Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        return 0;
    }

    @Override
    public int delete (Uri uri, String selection, String[] selectionArgs)
    {
        return 0;
    }

    @Override
    public String getType (Uri uri)
    {
        return null;
    }

    @Override
    public boolean onCreate()
    {
        return true;
    }
}
