package com.khmlabs.synctab.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.khmlabs.synctab.AppConstants;
import com.khmlabs.synctab.db.DbMetadata.QueueTasksColumns;
import com.khmlabs.synctab.db.DbMetadata.SharedTabsColumns;
import com.khmlabs.synctab.db.DbMetadata.Table;
import com.khmlabs.synctab.db.DbMetadata.TagsColumns;

class DbOpenHelper extends SQLiteOpenHelper {

    private static int DB_VERSION = 2;

    private static int VER_TAGS = 2;

    private static String DB_NAME = "synctab.db";

    DbOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createSharedTabsTable());
        db.execSQL(createQueueTasksTable());
        db.execSQL(createTagsTable());

        db.execSQL(createSharedTabsIndex());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion == VER_TAGS) {
            // if upgrade to version 2 then:

            // add tag column to the SharedTabs table
            db.execSQL(sharedTabsAddTagColumn());

            // fix tab device value to be Android
            db.execSQL(fixTabDeviceName());

            // create a tags table
            db.execSQL(createTagsTable());

            // add 2nd param column to the queue_task table
            db.execSQL(queueTaskParam2Column());
        }
    }

    private String queueTaskParam2Column() {
        final StringBuilder builder = new StringBuilder(50);

        builder
                .append("alter table ").append(DbMetadata.Table.QUEUE_TASKS)
                .append(" add column ").append(QueueTasksColumns.PARAM_2).append(" text");

        return builder.toString();
    }

    private String fixTabDeviceName() {
        final StringBuilder builder = new StringBuilder(50);

        builder
                .append("update table ").append(DbMetadata.Table.SHARED_TABS)
                .append(" set ").append(SharedTabsColumns.DEVICE)
                .append(" = '").append(AppConstants.DEVICE_NAME).append("'");

        return builder.toString();
    }

    private String sharedTabsAddTagColumn() {
        final StringBuilder builder = new StringBuilder(50);

        builder
                .append("alter table ").append(DbMetadata.Table.SHARED_TABS)
                .append(" add column ").append(SharedTabsColumns.TAG).append(" text");

        return builder.toString();
    }

    private String createSharedTabsIndex() {
        final StringBuilder builder = new StringBuilder(50);

        // -- index by timestamp to sort faster
        builder // {
                .append("create index IX_").append(Table.SHARED_TABS)
                .append("_TS on ").append(Table.SHARED_TABS)
                .append("(").append(SharedTabsColumns.TIMESTAMP).append(");");
        // }

        return builder.toString();
    }

    private String createSharedTabsTable() {
        final StringBuilder builder = new StringBuilder(150);
        builder
                .append("create table ").append(DbMetadata.Table.SHARED_TABS).append("(")
                .append(DbMetadata.ID).append(" integer primary key,")
                .append(SharedTabsColumns.TAB_ID).append(" text unique,")
                .append(SharedTabsColumns.LINK).append(" text,")
                .append(SharedTabsColumns.TITLE).append(" text,")
                .append(SharedTabsColumns.TAG).append(" text,")
                .append(SharedTabsColumns.DEVICE).append(" text,")
                .append(SharedTabsColumns.FAVICON).append(" text,")
                .append(DbMetadata.SharedTabsColumns.TIMESTAMP).append(" long);");

        return builder.toString();
    }

    private String createTagsTable() {
        final StringBuilder builder = new StringBuilder(150);
        builder
                .append("create table ").append(DbMetadata.Table.TAGS).append("(")
                .append(DbMetadata.ID).append(" integer primary key,")
                .append(TagsColumns.ID).append(" text unique,")
                .append(TagsColumns.NAME).append(" text);");

        return builder.toString();
    }

    private String createQueueTasksTable() {
        final StringBuilder builder = new StringBuilder(150);
        builder
                .append("create table ").append(DbMetadata.Table.QUEUE_TASKS).append("(")
                .append(DbMetadata.ID).append(" integer primary key,")
                .append(QueueTasksColumns.TYPE).append(" integer,")
                .append(QueueTasksColumns.PARAM).append(" text,")
                .append(QueueTasksColumns.PARAM_2).append(" text);");

        return builder.toString();
    }
}