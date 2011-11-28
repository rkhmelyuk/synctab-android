package com.khmlabs.synctab.ui;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.khmlabs.synctab.R;
import com.khmlabs.synctab.db.DbMetadata.TagsColumns;
import com.khmlabs.synctab.db.SyncTabDatabase;

/**
 * Activity to edit the list of tags: add new, rename and remove some.
 */
public class TagEditActivity extends BaseUserActivity {

    private static final String TAG = "TagEditActivity";

    static final String[] ADAPTER_FROM = { TagsColumns.NAME };

    static final int[] ADAPTER_TO = { R.id.tag_name };

    ListView tags;
    SyncTabDatabase database;
    SimpleCursorAdapter tagsAdapter;

    boolean refreshing = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_tagedit);

        tags = (ListView) findViewById(R.id.tags);

        database = new SyncTabDatabase(this);
    }

    protected void onResume() {
        super.onResume();

        if (getSyncTabApplication().isAuthenticated()) {
            refreshTags();
        }
    }

    private void refreshTags() {
        if (!refreshing) {
            refreshAdapter();
            new RefreshTagsListTask().execute();
        }
    }

    private void refreshAdapter() {
        final Cursor cursor = database.findTags();
        startManagingCursor(cursor);

        if (tagsAdapter == null) {
            tagsAdapter = new SimpleCursorAdapter(
                    this, R.layout.tag_row,
                    cursor, ADAPTER_FROM, ADAPTER_TO);

            tags.setAdapter(tagsAdapter);
        }
        else {
            tagsAdapter.getCursor().requery();
            tagsAdapter.notifyDataSetChanged();
        }
    }

    private class RefreshTagsListTask extends AsyncTask<Void, Void, Boolean> {

        protected Boolean doInBackground(Void... voids) {
            try {
                return getSyncTabApplication().getFacade().refreshTags();
            }
            catch (Exception e) {
                Log.e(TAG, "Error to refresh list of tags", e);
                return false;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            refreshing = true;
        }

        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (result) {
                refreshAdapter();
            }

            refreshing = false;
        }
    }
}
