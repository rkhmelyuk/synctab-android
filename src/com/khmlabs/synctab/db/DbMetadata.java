package com.khmlabs.synctab.db;

import android.provider.BaseColumns;

public interface DbMetadata {

    String SHARED_TABS_TABLE = "shared_tabs";

    String ID = BaseColumns._ID;
    String TAB_ID = "tab_id";
    String TITLE = "title";
    String LINK = "link";
    String TIMESTAMP = "timestamp";
}
