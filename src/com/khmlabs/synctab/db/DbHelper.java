package com.khmlabs.synctab.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.khmlabs.synctab.tab.SharedTab;

import java.util.List;

public class DbHelper {

    private DbOpenHelper dbOpenHelper;

    private SQLiteDatabase readableDb;

    public DbHelper(Context context) {
        dbOpenHelper = new DbOpenHelper(context);
    }

    public SQLiteDatabase getReadableDatabase() {
        if (readableDb == null) {
            readableDb = dbOpenHelper.getReadableDatabase();
        }

        return readableDb;
    }

    public void close() {
        if (readableDb != null && readableDb.isOpen()) {
            readableDb.close();
            readableDb = null;
        }
    }

    public Cursor findSharedTabs() {
        return getReadableDatabase().query(
                DbMetadata.SHARED_TABS_TABLE,
                null, null, null, null, null,
                DbMetadata.ID + " DESC");
    }

    public void insertSharedTabs(List<SharedTab> tabs) {
        if (tabs != null && tabs.size() > 0) {
            SQLiteDatabase db = null;
            try {
                db = dbOpenHelper.getWritableDatabase();
                db.beginTransaction();

                ContentValues values = new ContentValues();
                for (SharedTab each : tabs) {
                    values.put(DbMetadata.TAB_ID, each.getId());
                    values.put(DbMetadata.LINK, each.getLink());
                    values.put(DbMetadata.TIMESTAMP, each.getTimestamp());
                    values.put(DbMetadata.TITLE, each.getTitle());

                    db.insertOrThrow(DbMetadata.SHARED_TABS_TABLE, null, values);

                    values.clear();
                }

                db.setTransactionSuccessful();
            }
            finally {
                if (db != null && db.isOpen()) {
                    if (db.inTransaction()) {
                        db.endTransaction();
                    }
                    db.close();
                }
            }
        }
    }
}
