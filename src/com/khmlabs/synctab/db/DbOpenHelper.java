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
        else if (oldVersion == 3) {
            db.execSQL(addFaviconColumn());
        }
    }

    private String addFaviconColumn() {
        return "alter table " + DbMetadata.SHARED_TABS_TABLE + " add column " + DbMetadata.FAVICON + " text";
    }

    private String createSharedTabsTable() {
        final StringBuilder builder = new StringBuilder(150);
        builder
                .append("create table ").append(DbMetadata.SHARED_TABS_TABLE).append("(")
                .append(DbMetadata.ID).append(" integer primary key,")
                .append(DbMetadata.TAB_ID).append(" text unique,")
                .append(DbMetadata.LINK).append(" text,")
                .append(DbMetadata.TITLE).append(" text,")
                .append(DbMetadata.DEVICE).append(" text,")
                .append(DbMetadata.FAVICON).append(" text,")
                .append(DbMetadata.TIMESTAMP).append(" long);");

        return builder.toString();
    }

    private String createQueueTasksTable() {
        final StringBuilder builder = new StringBuilder(150);
        builder
                .append("create table ").append(DbMetadata.QUEUE_TASK_TABLE).append("(")
                .append(DbMetadata.ID).append(" integer primary key,")
                .append(DbMetadata.TYPE).append(" integer,")
                .append(DbMetadata.PARAM).append(" text);");

        return builder.toString();
    }

    private String dropSharedTabsTable() {
        final StringBuilder builder = new StringBuilder(40);
        builder.append("drop table ").append(DbMetadata.SHARED_TABS_TABLE).append(";");
        return builder.toString();
    }

    private String dropQueueTasksTable() {
        final StringBuilder builder = new StringBuilder(40);
        builder.append("drop table ").append(DbMetadata.QUEUE_TASK_TABLE).append(";");
        return builder.toString();
    }
}