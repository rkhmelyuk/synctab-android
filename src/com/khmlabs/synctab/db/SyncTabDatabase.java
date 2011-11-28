package com.khmlabs.synctab.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.khmlabs.synctab.db.DbMetadata.QueueTasksColumns;
import com.khmlabs.synctab.db.DbMetadata.SharedTabsColumns;
import com.khmlabs.synctab.db.DbMetadata.Table;
import com.khmlabs.synctab.db.DbMetadata.TagsColumns;
import com.khmlabs.synctab.queue.QueueTask;
import com.khmlabs.synctab.queue.TaskType;
import com.khmlabs.synctab.tab.SharedTab;
import com.khmlabs.synctab.tag.Tag;
import com.khmlabs.synctab.util.StringUtil;

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
                values.put(QueueTasksColumns.PARAM, task.getParam1());
                values.put(QueueTasksColumns.PARAM_2, task.getParam2());

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
                    values.put(SharedTabsColumns.LINK, each.getLink());
                    values.put(SharedTabsColumns.TIMESTAMP, each.getTimestamp());
                    values.put(SharedTabsColumns.TITLE, each.getTitle());
                    values.put(SharedTabsColumns.TAG, each.getTagId());
                    values.put(SharedTabsColumns.DEVICE, each.getDevice());
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

                values.put(SharedTabsColumns.TAB_ID, tab.getId());
                values.put(SharedTabsColumns.LINK, tab.getLink());
                values.put(SharedTabsColumns.TIMESTAMP, tab.getTimestamp());
                values.put(SharedTabsColumns.TITLE, tab.getTitle());
                values.put(SharedTabsColumns.TAG, tab.getTagId());
                values.put(SharedTabsColumns.DEVICE, tab.getDevice());
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

        Cursor cursor = null;

        try {
            final SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(Table.QUEUE_TASKS,
                    null, null, null, null, null, null);

            while (cursor.moveToNext()) {
                final QueueTask task = new QueueTask();
                task.setId(cursor.getInt(0));
                task.setType(TaskType.findById(cursor.getInt(1)));
                task.setParam1(cursor.getString(2));
                task.setParam2(cursor.getString(3));

                result.add(task);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
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

        Cursor cursor = null;

        try {
            cursor = db.query(Table.SHARED_TABS, null,
                    DbMetadata.ID + "=" + id, null, null, null, null);

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
            if (cursor != null) {
                cursor.close();
            }
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

    public void replaceTags(List<Tag> tags) {
        if (tags != null && tags.size() > 0) {
            SQLiteDatabase db = null;
            try {
                db = dbOpenHelper.getWritableDatabase();
                db.beginTransaction();

                final ContentValues values = new ContentValues();
                for (Tag each : tags) {
                    values.put(TagsColumns.ID, each.getTagId());
                    values.put(TagsColumns.NAME, each.getName());
                    db.replaceOrThrow(Table.TAGS, null, values);

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

    public Cursor findTags() {
        return getReadableDatabase().query(
                DbMetadata.Table.TAGS,
                null, null, null, null, null, null);
    }

    public List<Tag> getTags() {
        final List<Tag> result = new ArrayList<Tag>();

        Cursor cursor = null;
        try {
            final SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(Table.TAGS, null, null, null, null, null, null);

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(DbMetadata.ID));
                String tagId = cursor.getString(cursor.getColumnIndex(TagsColumns.ID));
                String name = cursor.getString(cursor.getColumnIndex(TagsColumns.NAME));

                result.add(new Tag(id, tagId, name));
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
    }

    public Tag getTag(int id) {
        final SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = null;

        try {
            cursor = db.query(Table.TAGS, null,
                    DbMetadata.ID + "=" + Integer.toString(id),
                    null, null, null, null);

            if (cursor.moveToNext()) {
                final Tag tag = new Tag();

                tag.setId(cursor.getInt(0));
                tag.setTagId(cursor.getString(cursor.getColumnIndex(TagsColumns.ID)));
                tag.setName(cursor.getString(cursor.getColumnIndex(TagsColumns.NAME)));

                return tag;
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    public Tag getTagById(String id) {
        final SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = null;

        try {
            cursor = db.query(Table.TAGS, null,
                    TagsColumns.ID + "='" + id + "'",
                    null, null, null, null);

            if (cursor.moveToNext()) {
                final Tag tag = new Tag();

                tag.setId(cursor.getInt(0));
                tag.setTagId(cursor.getString(cursor.getColumnIndex(TagsColumns.ID)));
                tag.setName(cursor.getString(cursor.getColumnIndex(TagsColumns.NAME)));

                return tag;
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    public Tag addTag(String tagId, String name) {
        Tag tag = null;

        if (StringUtil.isNotEmpty(name)) {
            SQLiteDatabase db = null;
            try {
                db = dbOpenHelper.getWritableDatabase();
                db.beginTransaction();

                final ContentValues values = new ContentValues();
                values.put(TagsColumns.ID, tagId);
                values.put(TagsColumns.NAME, name);

                long id = db.replaceOrThrow(Table.TAGS, null, values);
                if (id != -1) {
                    tag = new Tag((int) id, null, name);
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

        return tag;
    }

    public void updateTag(Tag tag) {
        if (tag != null) {
            SQLiteDatabase db = null;
            try {
                db = dbOpenHelper.getWritableDatabase();
                db.beginTransaction();

                final ContentValues values = new ContentValues();
                values.put(TagsColumns.ID, tag.getId());
                values.put(TagsColumns.NAME, tag.getName());

                db.update(Table.TAGS, values, DbMetadata.ID + "=" + Integer.toString(tag.getId()), null);

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

    public void removeTag(int tagId) {
        SQLiteDatabase db = null;
        try {
            db = dbOpenHelper.getWritableDatabase();
            db.beginTransaction();

            db.delete(Table.TAGS, DbMetadata.ID + "=" + Integer.toString(tagId), null);

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
