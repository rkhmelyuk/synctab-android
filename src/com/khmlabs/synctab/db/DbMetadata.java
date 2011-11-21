package com.khmlabs.synctab.db;

import android.provider.BaseColumns;

public interface DbMetadata {

    String ID = BaseColumns._ID;

    public interface Table {
        String SHARED_TABS = "shared_tabs";
        String QUEUE_TASKS = "queue_tasks";
        String TAGS = "tags";
    }

    public interface SharedTabsColumns {
        String TAB_ID = "tab_id";
        String TITLE = "title";
        String LINK = "link";
        String TIMESTAMP = "timestamp";
        String TAG = "tag";
        String FAVICON = "favicon";
    }

    public interface QueueTasksColumns {
        String TYPE = "type";
        String PARAM = "param";
    }

    public interface TagsColumns {
        String ID = "id";
        String NAME = "name";
    }

}
