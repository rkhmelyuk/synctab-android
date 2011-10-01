package com.khmlabs.synctab.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.khmlabs.synctab.queue.QueueTask;
import com.khmlabs.synctab.queue.TaskType;
import com.khmlabs.synctab.tab.SharedTab;

import java.util.ArrayList;
import java.util.List;

public class DbHelper {

    private DbOpenHelper dbOpenHelper;

    private SQLiteDatabase readableDb;

    public DbHelper(Context context) {
        dbOpenHelper = new DbOpenHelper(context);
        dbOpenHelper.getWritableDatabase().close();
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

    public void insertQueueTask(QueueTask task) {
        if (task != null) {
            SQLiteDatabase db = null;
            try {
                db = dbOpenHelper.getWritableDatabase();
                db.beginTransaction();

                ContentValues values = new ContentValues();

                values.put(DbMetadata.TYPE, task.getType().getId());
                values.put(DbMetadata.PARAM, task.getParam());

                db.insertOrThrow(DbMetadata.QUEUE_TASK_TABLE, null, values);

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

    public List<QueueTask> getQueuedTasks() {
        final List<QueueTask> result = new ArrayList<QueueTask>();
        final SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(DbMetadata.QUEUE_TASK_TABLE, null, null, null, null, null, null);

        try {
            while (cursor.moveToNext()) {
                QueueTask task = new QueueTask();
                task.setId(cursor.getInt(0));
                task.setType(TaskType.findById(cursor.getInt(1)));
                task.setParam(cursor.getString(2));

                result.add(task);
            }
        }
        finally {
            cursor.close();
        }

        return result;
    }

    public void removeQueueTask(QueueTask task) {
        SQLiteDatabase db = null;
        try {
            db = dbOpenHelper.getWritableDatabase();
            db.beginTransaction();

            db.delete(DbMetadata.QUEUE_TASK_TABLE, DbMetadata.ID + "="+ task.getId(), null);

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

    public void removeUserData() {
        SQLiteDatabase db = null;
        try {
            db = dbOpenHelper.getWritableDatabase();
            db.beginTransaction();

            db.delete(DbMetadata.QUEUE_TASK_TABLE, null, null);
            db.delete(DbMetadata.SHARED_TABS_TABLE, null, null);

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
