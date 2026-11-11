package com.example.stepbystep.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** SQLite storage for finished StepByStep sessions. */
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "stepbystep.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_ACTIVITIES = "activities";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_STEPS = "steps";
    public static final String COLUMN_SHAKES = "shakes";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_DATE_TIME = "date_time";

    public DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_ACTIVITIES + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_STEPS + " INTEGER NOT NULL, "
                + COLUMN_SHAKES + " INTEGER NOT NULL, "
                + COLUMN_DURATION + " INTEGER NOT NULL, "
                + COLUMN_DATE_TIME + " TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 has no migrations. Future changes should migrate existing data here.
    }
}
