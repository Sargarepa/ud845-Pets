package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.pets.R;
import com.example.android.pets.data.PetContract.PetEntry;

public class PetProvider extends ContentProvider {
    public static final String LOG_TAG = PetProvider.class.getSimpleName();

    public static final String CONTENT_LIST_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
                                                PetContract.CONTENT_AUTHORITY + "/" +
                                                PetContract.PATH_PETS;

    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" +
                                                PetContract.CONTENT_AUTHORITY + "/" +
                                                PetContract.PATH_PETS;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private PetDbHelper mDbHelper;

    private static final int PETS = 100;
    private static final int PET_ID = 101;

    static {
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS + "/#", PET_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new PetDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                cursor = db.query(PetEntry.TABLE_NAME, strings, s, strings1, null, null, s1);
                break;
            case PET_ID:
                s = PetEntry._ID + "?=";
                strings1 = new String[] {String.valueOf(ContentUris.parseId(uri))};
                cursor = db.query(PetEntry.TABLE_NAME, strings, s, strings1, null, null, s1);
            break;
            default:
                throw new IllegalArgumentException(R.string.err_unknown_query + String.valueOf(uri));
        }

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch(match) {
            case PETS:
                return CONTENT_LIST_TYPE;
            case PET_ID:
                return CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return insertPet(uri, contentValues);
            default:
                throw new IllegalArgumentException(getContext().getResources().getString(R.string.err_unknown_query) + uri);
        }
    }

    private Uri insertPet(Uri uri, ContentValues contentValues) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        String name = contentValues.getAsString(PetEntry.COLUMN_PET_NAME);
        if (name == null) {
            throw new IllegalArgumentException(getContext().getResources().getString(R.string.err_no_pet_name));
        }

        Integer gender = contentValues.getAsInteger(PetEntry.COLUMN_PET_GENDER);
        if (gender == null || !PetContract.isValidGender(gender)) {
            throw new IllegalArgumentException(getContext().getResources().getString(R.string.err_invalid_gender));
        }

        Integer weight = contentValues.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
        if (weight != null && weight <= 0) {
            throw new IllegalArgumentException(getContext().getResources().getString(R.string.err_invalid_weight));
        }

        long newRowId = db.insert(PetEntry.TABLE_NAME, null, contentValues);

        if (newRowId == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        return ContentUris.withAppendedId(uri, newRowId);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return delete(uri, s, strings);
            case PET_ID:
                s = PetEntry._ID + "?=";
                strings = new String[] {String.valueOf(ContentUris.parseId(uri))};
                return delete(uri, s, strings);
            default:
                throw new IllegalArgumentException(getContext().getResources().getString(R.string.err_delete_not_supported));
        }
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return updatePet(uri, contentValues, s, strings);
            case PET_ID:
                s = PetEntry._ID + "?=";
                strings = new String[] {String.valueOf(ContentUris.parseId(uri))};
                return updatePet(uri, contentValues, s, strings);
            default:
                throw new IllegalArgumentException(getContext().getResources().getString(R.string.err_update_not_supported) + uri);
        }
    }

    private int updatePet(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {


        if (contentValues.containsKey(PetEntry.COLUMN_PET_NAME)) {
            String name = contentValues.getAsString(PetEntry.COLUMN_PET_NAME);
            if (name == null) {
                throw new IllegalArgumentException(getContext().getResources().getString(R.string.err_no_pet_name));
            }
        }

        if (contentValues.containsKey(PetEntry.COLUMN_PET_GENDER)) {
            Integer gender = contentValues.getAsInteger(PetEntry.COLUMN_PET_GENDER);
            if (gender == null || !PetContract.isValidGender(gender)) {
                throw new IllegalArgumentException(getContext().getResources().getString(R.string.err_invalid_gender));
            }
        }

        if (contentValues.containsKey(PetEntry.COLUMN_PET_WEIGHT)) {
            Integer weight = contentValues.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
            if (weight != null && weight <= 0) {
                throw new IllegalArgumentException(getContext().getResources().getString(R.string.err_invalid_weight));
            }
        }

        if (contentValues.size() == 0) {
            return 0;
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        return db.update(PetEntry.TABLE_NAME, contentValues, selection, selectionArgs);
    }
}
