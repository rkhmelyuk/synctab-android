package com.khmlabs.synctab.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DbOpenHelper extends SQLiteOpenHelper {

    private static int DB_VERSION = 1;

    private static String DB_NAME = "synctab.db";

    DbOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createSharedTabsTable());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(dropSharedTabsTable());
        db.execSQL(createSharedTabsTable());
    }

    private String createSharedTabsTable() {
        final StringBuilder builder = new StringBuilder(150);
        builder
                .append("create table ").append(DbMetadata.SHARED_TABS_TABLE).append("(")
                .append(DbMetadata.ID).append(" int primary key,")
                .append(DbMetadata.TAB_ID).append(" text,")
                .append(DbMetadata.LINK).append(" text,")
                .append(DbMetadata.TITLE).append(" text);")
                .append(DbMetadata.TIMESTAMP).append(" long);");

        return builder.toString();
    }

    private String dropSharedTabsTable() {
        final StringBuilder builder = new StringBuilder(40);
        builder.append("drop table ").append(DbMetadata.SHARED_TABS_TABLE).append(";");
        return builder.toString();
    }
}