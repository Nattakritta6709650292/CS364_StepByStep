package com.example.stepbystep.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.stepbystep.session.SessionData;

import java.util.ArrayList;
import java.util.List;

/** Data access for completed sessions. Call database operations off the UI thread. */
public class ActivityRepository {
    private final DatabaseHelper helper;

    public ActivityRepository(Context context) {
        helper = new DatabaseHelper(context);
    }

    /** Returns new row ID, or -1 on insert failure. */
    public long insertActivity(SessionData data) {
        if (data == null) throw new IllegalArgumentException("SessionData cannot be null");
        if (data.steps < 0 || data.shakes < 0 || data.duration < 0
                || data.dateTime == null || data.dateTime.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid session values");
        }
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_STEPS, data.steps);
        values.put(DatabaseHelper.COLUMN_SHAKES, data.shakes);
        values.put(DatabaseHelper.COLUMN_DURATION, data.duration);
        values.put(DatabaseHelper.COLUMN_DATE_TIME, data.dateTime);
        return helper.getWritableDatabase().insert(DatabaseHelper.TABLE_ACTIVITIES, null, values);
    }

    /** Newest sessions first; each row becomes an independent SessionData. */
    public List<SessionData> getAllActivities() {
        List<SessionData> results = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query(
                DatabaseHelper.TABLE_ACTIVITIES,
                new String[]{DatabaseHelper.COLUMN_STEPS, DatabaseHelper.COLUMN_SHAKES,
                        DatabaseHelper.COLUMN_DURATION, DatabaseHelper.COLUMN_DATE_TIME},
                null, null, null, null,
                DatabaseHelper.COLUMN_DATE_TIME + " DESC, " + DatabaseHelper.COLUMN_ID + " DESC")) {
            while (cursor.moveToNext()) results.add(readSession(cursor));
        }
        return results;
    }

    /** Returns null if no matching row exists. */
    public SessionData getActivityById(int id) {
        try (Cursor cursor = helper.getReadableDatabase().query(
                DatabaseHelper.TABLE_ACTIVITIES,
                new String[]{DatabaseHelper.COLUMN_STEPS, DatabaseHelper.COLUMN_SHAKES,
                        DatabaseHelper.COLUMN_DURATION, DatabaseHelper.COLUMN_DATE_TIME},
                DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null)) {
            return cursor.moveToFirst() ? readSession(cursor) : null;
        }
    }

    /** Number of completed sessions. */
    public int getTotalSessions() {
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ACTIVITIES, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public int getTotalSteps() {
        return (int) Math.min(Integer.MAX_VALUE, getSum(DatabaseHelper.COLUMN_STEPS));
    }

    public int getTotalShakes() {
        return (int) Math.min(Integer.MAX_VALUE, getSum(DatabaseHelper.COLUMN_SHAKES));
    }

    private long getSum(String column) {
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(" + column + "), 0) FROM "
                        + DatabaseHelper.TABLE_ACTIVITIES, null)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        }
    }

    private SessionData readSession(Cursor cursor) {
        return new SessionData(cursor.getInt(0), cursor.getInt(1),
                cursor.getLong(2), cursor.getString(3));
    }

    public void close() {
        helper.close();
    }
}
