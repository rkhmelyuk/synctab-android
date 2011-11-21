package com.khmlabs.synctab.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.khmlabs.synctab.db.DbMetadata.QueueTasksColumns;
import com.khmlabs.synctab.db.DbMetadata.SharedTabsColumns;
import com.khmlabs.synctab.db.DbMetadata.Table;
import com.khmlabs.synctab.queue.QueueTask;
import com.khmlabs.synctab.queue.TaskType;
import com.khmlabs.synctab.tab.SharedTab;

import java.util.ArrayList;
import java.util.List;

public class SyncTabDatabase {

    private DbOpenHelper dbOpenHelper;

    private SQLiteDatabase readableDb;

    public SyncTabDatabase(Context context) {
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
                DbMetadata.Table.SHARED_TABS,
                null, null, null, null, null,
                SharedTabsColumns.TIMESTAMP + " DESC");
    }

    public void insertQueueTask(QueueTask task) {
        if (task != null) {
            SQLiteDatabase db = null;
            try {
                db = dbOpenHelper.getWritableDatabase();
                db.beginTransaction();

                final ContentValues values = new ContentValues();
                values.put(QueueTasksColumns.TYPE, task.getType().getId());
                values.put(QueueTasksColumns.PARAM, task.getParam());

                db.insertOrThrow(Table.QUEUE_TASKS, null, values);
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

    public void replaceSharedTabs(List<SharedTab> tabs) {
        if (tabs != null && tabs.size() > 0) {
            SQLiteDatabase db = null;
            try {
                db = dbOpenHelper.getWritableDatabase();
                db.beginTransaction();

                final ContentValues values = new ContentValues();
                for (SharedTab each : tabs) {
                    values.put(SharedTabsColumns.TAB_ID, each.getId());
                    values.put(DbMetadata.SharedTabsColumns.LINK, each.getLink());
                    values.put(SharedTabsColumns.TIMESTAMP, each.getTimestamp());
                    values.put(SharedTabsColumns.TITLE, each.getTitle());
                    values.put(SharedTabsColumns.TAG, each.getTagId());
                    values.put(SharedTabsColumns.FAVICON, each.getFavicon());

                    db.replaceOrThrow(Table.SHARED_TABS, null, values);

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

    public void replaceSharedTab(SharedTab tab) {
        if (tab != null) {
            SQLiteDatabase db = null;
            try {
                db = dbOpenHelper.getWritableDatabase();
                db.beginTransaction();

                final ContentValues values = new ContentValues();

                values.put(DbMetadata.SharedTabsColumns.TAB_ID, tab.getId());
                values.put(SharedTabsColumns.LINK, tab.getLink());
                values.put(SharedTabsColumns.TIMESTAMP, tab.getTimestamp());
                values.put(SharedTabsColumns.TITLE, tab.getTitle());
                values.put(SharedTabsColumns.TAG, tab.getTagId());
                values.put(SharedTabsColumns.FAVICON, tab.getFavicon());

                db.replaceOrThrow(Table.SHARED_TABS, null, values);
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
        final Cursor cursor = db.query(Table.QUEUE_TASKS,
                null, null, null, null, null, null);

        try {
            while (cursor.moveToNext()) {
                final QueueTask task = new QueueTask();
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
        removeById(Table.QUEUE_TASKS, task.getId());
    }

    /**
     * Remove all user data.
     * Used to cleanup current user data on cleanup.
     */
    public void removeUserData() {
        SQLiteDatabase db = null;
        try {
            db = dbOpenHelper.getWritableDatabase();
            db.beginTransaction();

            db.delete(Table.QUEUE_TASKS, null, null);
            db.delete(Table.SHARED_TABS, null, null);
            db.delete(Table.TAGS, null, null);

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

    public SharedTab getSharedTabById(int id) {
        final SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(Table.SHARED_TABS, null,
                DbMetadata.ID + "=" + id, null, null, null, null);

        try {
            if (cursor.moveToNext()) {
                final SharedTab tab = new SharedTab();

                tab.setRowId(cursor.getInt(0));
                tab.setId(cursor.getString(cursor.getColumnIndex(SharedTabsColumns.TAB_ID)));
                tab.setLink(cursor.getString(cursor.getColumnIndex(SharedTabsColumns.LINK)));
                tab.setTitle(cursor.getString(cursor.getColumnIndex(SharedTabsColumns.TITLE)));
                tab.setTagId(cursor.getString(cursor.getColumnIndex(SharedTabsColumns.TAG)));
                tab.setTimestamp(cursor.getLong(cursor.getColumnIndex(SharedTabsColumns.TIMESTAMP)));
                tab.setFavicon(cursor.getString(cursor.getColumnIndex(DbMetadata.SharedTabsColumns.FAVICON)));

                return tab;
            }
        }
        finally {
            cursor.close();
        }

        return null;
    }

    public void removeSharedTab(int id) {
        removeById(Table.SHARED_TABS, id);
    }

    private void removeById(String table, int id) {
        SQLiteDatabase db = null;
        try {
            db = dbOpenHelper.getWritableDatabase();
            db.beginTransaction();

            db.delete(table, DbMetadata.ID + "=" + Integer.toString(id), null);

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
