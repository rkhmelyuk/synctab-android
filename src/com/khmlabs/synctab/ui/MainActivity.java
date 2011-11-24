package com.khmlabs.synctab.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.khmlabs.synctab.*;
import com.khmlabs.synctab.db.DbMetadata;
import com.khmlabs.synctab.db.DbMetadata.SharedTabsColumns;
import com.khmlabs.synctab.db.SyncTabDatabase;
import com.khmlabs.synctab.util.IntentHelper;
import com.khmlabs.synctab.util.UrlUtil;

import java.io.InputStream;
import java.util.Date;

/**
 * This activity used to show and manipulate the list of shared tabs.
 */
public class MainActivity extends BaseUserActivity {

    private static final String TAG = "MainActivity";

    private static final int TAB_CONTEXT_MENU_RESEND = 0;
    private static final int TAB_CONTEXT_MENU_REMOVE = 1;
    private static final int TAB_CONTEXT_MENU_SHARE = 2;
    private static final int TAB_CONTEXT_MENU_COPY = 3;

    static final String[] ADAPTER_FROM = {
            SharedTabsColumns.FAVICON, SharedTabsColumns.TITLE,
            SharedTabsColumns.LINK, SharedTabsColumns.TIMESTAMP,
            SharedTabsColumns.DEVICE
    };

    static final int[] ADAPTER_TO = {
            R.id.tab_icon, R.id.tab_title,
            R.id.tab_link, R.id.tab_date,
            R.id.device
    };

    private SimpleCursorAdapter.ViewBinder ROW_BINDER;

    ListView sharedTabs;
    SimpleCursorAdapter sharedTabsAdapter;

    private SyncTabDatabase database;
    private boolean refreshing = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ROW_BINDER = new SharedTabsBinder(getSyncTabApplication());

        database = new SyncTabDatabase(this);

        sharedTabs = (ListView) findViewById(R.id.tabs);
        sharedTabs.setOnScrollListener(new AbsListView.OnScrollListener() {
            int prevTotalCount = 0;

            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
            }

            public void onScroll(AbsListView view, int firstVisibleItem, int visibleCount, int totalCount) {
                if (firstVisibleItem + visibleCount != 0) {
                    boolean needLoad = (5 + firstVisibleItem + visibleCount >= totalCount);
                    needLoad &= (prevTotalCount != totalCount);

                    if (needLoad) {
                        prevTotalCount = totalCount;
                        new LoadNextPageTask().execute();
                    }
                }
            }
        });

        sharedTabs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                final Cursor cursor = (Cursor) adapterView.getAdapter().getItem(position);
                final int linkColumn = cursor.getColumnIndex(SharedTabsColumns.LINK);
                final String link = cursor.getString(linkColumn);

                IntentHelper.browseLink(MainActivity.this, link);
            }
        });

        registerForContextMenu(sharedTabs);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getSyncTabApplication().isAuthenticated()) {
            refreshSharedTabs();

            // if not authenticated - then no cache
            // because we cleanup cache on logout
            new CleanupCacheTask().execute();
        }
    }

    @Override
    protected void onDestroy() {
        super.onStop();

        database.close();
    }

    private boolean refreshAdapter() {
        final Cursor cursor = database.findSharedTabs();
        startManagingCursor(cursor);

        if (sharedTabsAdapter == null) {
            sharedTabsAdapter = new SimpleCursorAdapter(
                    this, R.layout.tab_row,
                    cursor, ADAPTER_FROM, ADAPTER_TO);

            sharedTabsAdapter.setViewBinder(ROW_BINDER);
            sharedTabs.setAdapter(sharedTabsAdapter);
        }
        else {
            sharedTabsAdapter.getCursor().requery();
            sharedTabsAdapter.notifyDataSetChanged();
        }

        //
        // check if empty, hide list if so and return a result.

        boolean empty = (cursor.getCount() == 0);
        hideListIfEmpty(empty);

        return !empty;
    }

    private void hideListIfEmpty(boolean empty) {
        if (empty) {
            sharedTabs.setVisibility(View.GONE);
            findViewById(R.id.start_help_notice).setVisibility(View.VISIBLE);
        }
        else {
            sharedTabs.setVisibility(View.VISIBLE);
            findViewById(R.id.start_help_notice).setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        if (view.getId() == R.id.tabs) {
            final AdapterView.AdapterContextMenuInfo ctxMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
            final ListView listView = (ListView) view;

            setTabContextMenuHeader(listView, menu, ctxMenuInfo);

            menu.add(Menu.NONE, TAB_CONTEXT_MENU_RESEND, TAB_CONTEXT_MENU_RESEND,
                    getResources().getString(R.string.resend));

            menu.add(Menu.NONE, TAB_CONTEXT_MENU_REMOVE, TAB_CONTEXT_MENU_REMOVE,
                    getResources().getString(R.string.remove));

            menu.add(Menu.NONE, TAB_CONTEXT_MENU_SHARE, TAB_CONTEXT_MENU_SHARE,
                    getResources().getString(R.string.share_via));

            menu.add(Menu.NONE, TAB_CONTEXT_MENU_COPY, TAB_CONTEXT_MENU_COPY,
                    getResources().getString(R.string.copy_link));
        }
    }

    private void setTabContextMenuHeader(ListView listView, ContextMenu menu,
                                         AdapterView.AdapterContextMenuInfo ctxMenuInfo) {

        final Cursor cursor = (Cursor) listView.getAdapter().getItem(ctxMenuInfo.position);

        final int titleColumn = cursor.getColumnIndex(SharedTabsColumns.TITLE);
        final String title = cursor.getString(titleColumn);

        if (title != null && title.length() != 0) {
            menu.setHeaderTitle(Html.fromHtml(title).toString());
        }
        else {
            final int linkColumn = cursor.getColumnIndex(SharedTabsColumns.LINK);
            String link = cursor.getString(linkColumn);
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
            case TAB_CONTEXT_MENU_RESEND:
                new ReshareTabTask().execute(tabId);
                break;
            case TAB_CONTEXT_MENU_REMOVE:
                new RemoveTabTask().execute(tabId);
                break;
            case TAB_CONTEXT_MENU_SHARE:
                sendLink(cursor);
                break;
            case TAB_CONTEXT_MENU_COPY:
                copyLink(cursor);
                break;
        }

        return true;
    }

    private void sendLink(Cursor cursor) {
        final int linkColumn = cursor.getColumnIndex(SharedTabsColumns.LINK);
        final int titleColumn = cursor.getColumnIndex(SharedTabsColumns.TITLE);

        final String link = cursor.getString(linkColumn);
        final String title = cursor.getString(titleColumn);

        new SendTabTask().execute(link, title);
    }

    private void copyLink(Cursor cursor) {
        final int linkColumn = cursor.getColumnIndex(SharedTabsColumns.LINK);
        final String link = cursor.getString(linkColumn);

        final ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        manager.setText(link);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        if (!result) {
            switch (item.getItemId()) {
                case R.id.refresh: {
                    refreshSharedTabs();
                    return true;
                }
            }
        }
        return result;
    }

    public void refreshSharedTabs() {
        if (!refreshing) {
            // use some events
            boolean filled = refreshAdapter();
            new RefreshSharedTabsTask(filled).execute();
        }
    }

    void setRefreshing(boolean refreshing) {
        if (this.refreshing != refreshing) {
            this.refreshing = refreshing;

            if (refreshing) {
                titlebarHelper.setRefreshing(true);
            }
            else {
                titlebarHelper.setRefreshing(false);
            }
        }
    }

    private class RefreshSharedTabsTask extends AsyncTask<String, String, Boolean> {

        final boolean filled;

        ProgressDialog progress;

        private RefreshSharedTabsTask(boolean filled) {
            this.filled = filled;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (!filled) {
                final String message = getResources().getString(R.string.loading_tabs);
                progress = ProgressDialog.show(MainActivity.this, null, message, true, false);
            }
            else {
                setRefreshing(true);
            }
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            try {
                SyncTabApplication application = getSyncTabApplication();
                SyncTabFacade facade = application.getFacade();
                return facade.refreshSharedTabs();
            }
            catch (Exception e) {
                Log.e(MainActivity.TAG, "error to refresh shared tabs", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean status) {

            if (progress != null) {
                progress.dismiss();
                progress = null;
            }

            if (!status) {
                Toast.makeText(MainActivity.this,
                        R.string.failed_retrieve_shared_tabs, 5000);
            }
            else {
                refreshAdapter();
            }

            setRefreshing(false);
        }
    }

    private class RemoveTabTask extends AsyncTask<Integer, Integer, RemoteOpStatus> {
        @Override
        protected RemoteOpStatus doInBackground(Integer... params) {
            final int tabId = params[0];
            final SyncTabFacade facade = getSyncTabApplication().getFacade();

            return facade.removeSharedTab(tabId);
        }

        @Override
        protected void onPostExecute(RemoteOpStatus status) {
            final int messageId;
            if (status == RemoteOpStatus.Failed) {
                messageId = R.string.msg_failed_to_remove_tab;
            }
            else {
                messageId = R.string.msg_tab_is_removed;
                refreshSharedTabs();
            }

            String message = getResources().getString(messageId);
            Toast.makeText(MainActivity.this, message, 3000).show();
        }
    }

    private class ReshareTabTask extends AsyncTask<Integer, Integer, RemoteOpStatus> {

        @Override
        protected RemoteOpStatus doInBackground(Integer... params) {
            final int tabId = params[0];
            final SyncTabFacade facade = getSyncTabApplication().getFacade();
            return facade.reshareTab(tabId);
        }

        @Override
        protected void onPostExecute(RemoteOpStatus status) {
            final int messageId;
            if (status == RemoteOpStatus.Failed) {
                messageId = R.string.msg_failed_to_reshared_tab;
            }
            else {
                messageId = R.string.msg_tab_is_reshared;
                refreshSharedTabs();
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

            final Intent sendIntent = new Intent(Intent.ACTION_SEND);

            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, link);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, title);

            String chooserTitle = getResources().getString(R.string.share_via);
            startActivity(Intent.createChooser(sendIntent, chooserTitle));

            return true;
        }
    }

    private class CleanupCacheTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            getSyncTabApplication().cleanupCacheIfNeed();
            return true;
        }
    }

    private class LoadNextPageTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                SyncTabFacade facade = getSyncTabApplication().getFacade();
                return !facade.loadOlderSharedTabs();
            }
            catch (Exception e) {
                Log.e(MainActivity.TAG, "error to load older shared tabs", e);
            }
            return false;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            setRefreshing(true);
        }

        @Override
        protected void onPostExecute(Boolean empty) {
            super.onPostExecute(empty);

            setRefreshing(false);
            if (!empty) {
                refreshAdapter();
            }
        }
    }

    private static class SharedTabsBinder implements SimpleCursorAdapter.ViewBinder {

        private final SyncTabApplication app;
        private final FileCacheManager cacheManager;

        private SharedTabsBinder(SyncTabApplication app) {
            this.app = app;
            this.cacheManager = app.getCacheManager();
        }

        public boolean setViewValue(View element, Cursor cursor, int columnIndex) {
            if (element.getId() == R.id.tab_icon) {
                String favicon = cursor.getString(columnIndex);
                if (favicon != null && favicon.length() > 0) {
                    final InputStream image = cacheManager.read(favicon);
                    if (image != null) {
                        final Bitmap bitmap = BitmapFactory.decodeStream(image);
                        ((ImageView) element).setImageBitmap(bitmap);
                        element.setVisibility(View.VISIBLE);

                        return true;
                    }
                }

                ((ImageView) element).setImageDrawable(null);

                element.setVisibility(View.GONE);

                return true;
            }
            else if (element.getId() == R.id.tab_date) {
                Date date = new Date(cursor.getLong(columnIndex));
                ((TextView) element).setText(DateUtils.getRelativeTimeSpanString(
                        date.getTime(), System.currentTimeMillis(), 1000));

                return true;
            }
            else if (element.getId() == R.id.device) {
                String device = cursor.getString(columnIndex);
                if (device == null) {
                    device = app.getResources().getString(R.string.unknown);
                }
                ((TextView) element).setText(device);

                return true;
            }
            else if (element.getId() == R.id.tab_title) {
                String title = cursor.getString(columnIndex);

                if (title != null && title.length() > 0) {
                    title = prepareReadableTitle(title);
                    ((TextView) element).setText(title);
                    element.setVisibility(View.VISIBLE);
                }
                else {
                    element.setVisibility(View.GONE);
                }

                return true;
            }
            else if (element.getId() == R.id.tab_link) {
                String link = cursor.getString(columnIndex);

                if (link != null && link.length() > 0) {
                    int maxlength = app.getResources().getInteger(R.integer.link_max_size);
                    link = UrlUtil.prepareReadableUrl(link);
                    link = UrlUtil.shortenizeUrl(link, maxlength);

                    ((TextView) element).setText(UrlUtil.colorizeUrl(link));
                    element.setVisibility(View.VISIBLE);
                }
                else {
                    element.setVisibility(View.GONE);
                }

                return true;
            }

            return false;
        }

        private String prepareReadableTitle(String title) {
            return Html.fromHtml(title).toString();
        }
    }

}