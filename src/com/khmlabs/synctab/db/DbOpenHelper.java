package com.khmlabs.synctab.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.khmlabs.synctab.db.DbMetadata.QueueTasksColumns;
import com.khmlabs.synctab.db.DbMetadata.SharedTabsColumn;
import com.khmlabs.synctab.db.DbMetadata.Table;

class DbOpenHelper extends SQLiteOpenHelper {

    private static int DB_VERSION = 1;

    private static String DB_NAME = "synctab.db";

    DbOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createSharedTabsTable());
        db.execSQL(createQueueTasksTable());

        db.execSQL(createSharedTabsIndex());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // nothing yet
    }

    private String createSharedTabsIndex() {
        final StringBuilder builder = new StringBuilder(50);

        // -- index by timestamp to sort faster
        builder // {
                .append("create index IX_").append(Table.SHARED_TABS)
                .append("_TS on ").append(Table.SHARED_TABS)
                .append("(").append(SharedTabsColumn.TIMESTAMP).append(");");
        // }

        return builder.toString();
    }

    private String createSharedTabsTable() {
        final StringBuilder builder = new StringBuilder(150);
        builder
                .append("create table ").append(DbMetadata.Table.SHARED_TABS).append("(")
                .append(DbMetadata.ID).append(" integer primary key,")
                .append(SharedTabsColumn.TAB_ID).append(" text unique,")
                .append(SharedTabsColumn.LINK).append(" text,")
                .append(SharedTabsColumn.TITLE).append(" text,")
                .append(SharedTabsColumn.DEVICE).append(" text,")
                .append(SharedTabsColumn.FAVICON).append(" text,")
                .append(SharedTabsColumn.TIMESTAMP).append(" long);");

        return builder.toString();
    }

    private String createQueueTasksTable() {
        final StringBuilder builder = new StringBuilder(150);
        builder
                .append("create table ").append(DbMetadata.Table.QUEUE_TASKS).append("(")
                .append(DbMetadata.ID).append(" integer primary key,")
                .append(QueueTasksColumns.TYPE).append(" integer,")
                .append(QueueTasksColumns.PARAM).append(" text);");

        return builder.toString();
    }

    private String dropSharedTabsTable() {
        final StringBuilder builder = new StringBuilder(40);
        builder.append("drop table ").append(Table.SHARED_TABS).append(";");
        return builder.toString();
    }

    private String dropQueueTasksTable() {
        final StringBuilder builder = new StringBuilder(40);
        builder.append("drop table ").append(Table.QUEUE_TASKS).append(";");
        return builder.toString();
    }
}