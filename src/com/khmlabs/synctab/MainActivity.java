package com.khmlabs.synctab;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.khmlabs.synctab.db.DbHelper;
import com.khmlabs.synctab.db.DbMetadata;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    static final String[] ADAPTER_FROM = {/*DbMetadata.TITLE, */DbMetadata.LINK, DbMetadata.TIMESTAMP};
    static final int[] ADAPTER_TO = {/*R.id.tab_title, */R.id.tab_link, R.id.tab_date};

    private final static SimpleCursorAdapter.ViewBinder ROW_BINDER = new SimpleCursorAdapter.ViewBinder() {
        public boolean setViewValue(View element, Cursor cursor, int columnIndex) {
            if (element.getId() == R.id.tab_date) {
                Date date = new Date(cursor.getLong(columnIndex));
                ((TextView) element).setText(new SimpleDateFormat("dd MMMMM yyyy, HH:mm").format(date));

                return true;
            }
            return false;
        }
    };

    private ListView sharedTabs;

    private DbHelper dbHelper;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        sharedTabs = (ListView) findViewById(R.id.tabs);
        dbHelper = new DbHelper(this);

        sharedTabs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                final Cursor cursor = (Cursor) adapterView.getAdapter().getItem(position);
                final int linkColumn = cursor.getColumnIndex(DbMetadata.LINK);
                final String link = cursor.getString(linkColumn);

                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getSyncTabApplication().isAuthenticated()) {
            refreshAdapter();
            new RefreshSharedTabsTask().execute();
        }
    }

    @Override
    protected void onDestroy() {
        super.onStop();

        dbHelper.close();
    }

    private void refreshAdapter() {
        Cursor cursor = dbHelper.findSharedTabs();
        startManagingCursor(cursor);

        SimpleCursorAdapter sharedTabsAdapter = new SimpleCursorAdapter(
                this, R.layout.tab_row,
                cursor, ADAPTER_FROM, ADAPTER_TO);

        sharedTabsAdapter.setViewBinder(ROW_BINDER);
        sharedTabs.setAdapter(sharedTabsAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                new RefreshSharedTabsTask().execute();
                break;
            case R.id.logout:
                getSyncTabApplication().logout();
                showLogin();
                break;
        }
        return true;
    }

    private class RefreshSharedTabsTask extends AsyncTask<String, String, Boolean> {

        @Override
        protected Boolean doInBackground(String... strings) {
            try {
                SyncTabApplication application = getSyncTabApplication();
                SyncTabRemoteService service = application.getSyncTabRemoteService();
                return service.refreshSharedTabs();
            }
            catch (Exception e) {
                Log.e(MainActivity.TAG, "error to refresh shared tabs", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean status) {
            if (!status) {
                Toast.makeText(MainActivity.this,
                        R.string.failed_retrieve_shared_tabs, 5000);
            }
            else {
                refreshAdapter();
            }
        }
    }

}