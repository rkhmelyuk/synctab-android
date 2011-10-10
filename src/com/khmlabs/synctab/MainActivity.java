package com.khmlabs.synctab;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.khmlabs.synctab.db.DbHelper;
import com.khmlabs.synctab.db.DbMetadata;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    private static final int TAB_CONTEXT_MENU_RESHARE = 0;
    private static final int TAB_CONTEXT_MENU_REMOVE = 1;
    private static final int TAB_CONTEXT_MENU_SEND = 2;

    static final String[] ADAPTER_FROM = {DbMetadata.TITLE, DbMetadata.LINK, DbMetadata.TIMESTAMP, DbMetadata.DEVICE};
    static final int[] ADAPTER_TO = {R.id.tab_title, R.id.tab_link, R.id.tab_date, R.id.device};

    private final SimpleCursorAdapter.ViewBinder ROW_BINDER = new SimpleCursorAdapter.ViewBinder() {
        public boolean setViewValue(View element, Cursor cursor, int columnIndex) {
            if (element.getId() == R.id.tab_date) {
                Date date = new Date(cursor.getLong(columnIndex));
                ((TextView) element).setText(new SimpleDateFormat("dd MMMMM yyyy, HH:mm").format(date));

                return true;
            }
            else if (element.getId() == R.id.device) {
                String device = cursor.getString(columnIndex);

                final String deviceName;
                if (AppConstants.ANDROID_SYNCTAB_DEVICE.equals(device)) {
                    deviceName = "Android";
                }
                else {
                    deviceName = getResources().getString(R.string.unknown);
                }
                ((TextView) element).setText(deviceName);

                return true;
            }
            else if (element.getId() == R.id.tab_title) {
                String title = cursor.getString(columnIndex);

                if (title != null && title.length() > 0) {
                    // TODO - max length should be a integer value, that depends on display size
                    if (title.length() > 35) {
                        title = title.substring(0, 33) + "...";
                    }
                    ((TextView) element).setText(title);
                    element.setVisibility(View.VISIBLE);
                }
                else {
                    element.setVisibility(View.GONE);
                }

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

        registerForContextMenu(sharedTabs);
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
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        if (view.getId() == R.id.tabs) {
            final AdapterView.AdapterContextMenuInfo ctxMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
            final ListView listView = (ListView) view;

            setTabContextMenuHeader(listView, menu, ctxMenuInfo);

            menu.add(Menu.NONE, TAB_CONTEXT_MENU_RESHARE, TAB_CONTEXT_MENU_RESHARE,
                    getResources().getString(R.string.reshare));

            menu.add(Menu.NONE, TAB_CONTEXT_MENU_REMOVE, TAB_CONTEXT_MENU_REMOVE,
                    getResources().getString(R.string.remove));

            menu.add(Menu.NONE, TAB_CONTEXT_MENU_SEND, TAB_CONTEXT_MENU_SEND,
                    getResources().getString(R.string.send_to));
        }
    }

    private void setTabContextMenuHeader(ListView listView, ContextMenu menu,
                                         AdapterView.AdapterContextMenuInfo ctxMenuInfo) {

        final Cursor cursor = (Cursor) listView.getAdapter().getItem(ctxMenuInfo.position);

        final int titleColumn = cursor.getColumnIndex(DbMetadata.TITLE);
        final String title = cursor.getString(titleColumn);

        if (title != null && title.length() != 0) {
            menu.setHeaderTitle(title);
        }
        else {
            final int linkColumn = cursor.getColumnIndex(DbMetadata.LINK);
            final String link = cursor.getString(linkColumn);
            menu.setHeaderTitle(link);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo ctxMenuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final Cursor cursor = (Cursor) sharedTabs.getAdapter().getItem(ctxMenuInfo.position);

        final int idColumn = cursor.getColumnIndex(DbMetadata.ID);
        final int tabId = cursor.getInt(idColumn);

        switch (item.getItemId()) {
            case TAB_CONTEXT_MENU_RESHARE:
                new ReshareTabTask().execute(tabId);
                break;
            case TAB_CONTEXT_MENU_REMOVE:
                new RemoveTabTask().execute(tabId);
                break;
            case TAB_CONTEXT_MENU_SEND:
                final int linkColumn = cursor.getColumnIndex(DbMetadata.LINK);
                final int titleColumn = cursor.getColumnIndex(DbMetadata.TITLE);

                final String link = cursor.getString(linkColumn);
                final String title = cursor.getString(titleColumn);

                new SendTabTask().execute(link, title);

                break;
        }

        return true;
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

    private class RemoveTabTask extends AsyncTask<Integer, Integer, RemoteOpState> {
        @Override
        protected RemoteOpState doInBackground(Integer... params) {
            final int tabId = params[0];
            final SyncTabRemoteService service = getSyncTabApplication().getSyncTabRemoteService();

            return service.removeSharedTab(tabId);
        }

        @Override
        protected void onPostExecute(RemoteOpState state) {
            final int messageId;
            if (state == RemoteOpState.Failed) {
                messageId = R.string.msg_failed_to_remove_tab;
            }
            else {
                messageId = R.string.msg_tab_is_removed;
                new RefreshSharedTabsTask().execute();
            }

            String message = getResources().getString(messageId);
            Toast.makeText(MainActivity.this, message, 3000).show();
        }
    }

    private class ReshareTabTask extends AsyncTask<Integer, Integer, RemoteOpState> {

        @Override
        protected RemoteOpState doInBackground(Integer... params) {
            final int tabId = params[0];
            final SyncTabRemoteService service = getSyncTabApplication().getSyncTabRemoteService();
            return service.reshareTab(tabId);
        }

        @Override
        protected void onPostExecute(RemoteOpState state) {
            final int messageId;
            if (state == RemoteOpState.Failed) {
                messageId = R.string.msg_failed_to_reshared_tab;
            }
            else {
                messageId = R.string.msg_tab_is_reshared;
                new RefreshSharedTabsTask().execute();
            }

            String message = getResources().getString(messageId);
            Toast.makeText(MainActivity.this, message, 3000).show();
        }
    }

    private class SendTabTask extends AsyncTask<String, String, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            final String link = params[0];
            final String title = params[1];

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, link);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, title);

            String chooserTitle = getResources().getString(R.string.send_to);
            startActivity(Intent.createChooser(sendIntent, chooserTitle));

            return true;
        }

    }

}