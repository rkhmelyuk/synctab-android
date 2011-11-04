package com.khmlabs.synctab.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DbOpenHelper extends SQLiteOpenHelper {

    private static int DB_VERSION = 4;

    private static String DB_NAME = "synctab.db";

    DbOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createSharedTabsTable());
        db.execSQL(createQueueTasksTable());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 2) {
            db.execSQL(dropSharedTabsTable());
            db.execSQL(createSharedTabsTable());
        }
    }

    private String createSharedTabsTable() {
        final StringBuilder builder = new StringBuilder(150);
        builder
                .append("create table ").append(DbMetadata.Table.SHARED_TABS).append("(")
                .append(DbMetadata.ID).append(" integer primary key,")
                .append(DbMetadata.SharedTabsColumn.TAB_ID).append(" text unique,")
                .append(DbMetadata.SharedTabsColumn.LINK).append(" text,")
                .append(DbMetadata.SharedTabsColumn.TITLE).append(" text,")
                .append(DbMetadata.SharedTabsColumn.DEVICE).append(" text,")
                .append(DbMetadata.SharedTabsColumn.FAVICON).append(" text,")
                .append(DbMetadata.SharedTabsColumn.TIMESTAMP).append(" long);");

        return builder.toString();
    }

    private String createQueueTasksTable() {
        final StringBuilder builder = new StringBuilder(150);
        builder
                .append("create table ").append(DbMetadata.Table.QUEUE_TASKS).append("(")
                .append(DbMetadata.ID).append(" integer primary key,")
                .append(DbMetadata.QueueTasksColumns.TYPE).append(" integer,")
                .append(DbMetadata.QueueTasksColumns.PARAM).append(" text);");

        return builder.toString();
    }

    private String dropSharedTabsTable() {
        final StringBuilder builder = new StringBuilder(40);
        builder.append("drop table ").append(DbMetadata.Table.SHARED_TABS).append(";");
        return builder.toString();
    }

    private String dropQueueTasksTable() {
        final StringBuilder builder = new StringBuilder(40);
        builder.append("drop table ").append(DbMetadata.Table.QUEUE_TASKS).append(";");
        return builder.toString();
    }
}