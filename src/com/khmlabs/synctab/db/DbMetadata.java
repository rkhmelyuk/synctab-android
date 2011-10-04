package com.khmlabs.synctab.db;

import android.provider.BaseColumns;

public interface DbMetadata {

    String SHARED_TABS_TABLE = "shared_tabs";
    String QUEUE_TASK_TABLE = "queue_tasks";

    String ID = BaseColumns._ID;
    String TAB_ID = "tab_id";
    String TITLE = "title";
    String LINK = "link";
    String TIMESTAMP = "timestamp";
    String DEVICE = "device";

    String TYPE = "type";
    String PARAM = "param";
}
