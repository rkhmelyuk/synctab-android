package com.khmlabs.synctab.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.khmlabs.synctab.*;
import com.khmlabs.synctab.db.DbHelper;
import com.khmlabs.synctab.db.DbMetadata;
import com.khmlabs.synctab.util.UrlUtil;

import java.io.InputStream;
import java.util.Date;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    private static final int TAB_CONTEXT_MENU_RESHARE = 0;
    private static final int TAB_CONTEXT_MENU_REMOVE = 1;
    private static final int TAB_CONTEXT_MENU_SEND = 2;
    private static final int TAB_CONTEXT_MENU_COPY = 3;

    static final String[] ADAPTER_FROM = {DbMetadata.FAVICON, DbMetadata.TITLE, DbMetadata.LINK, DbMetadata.TIMESTAMP, DbMetadata.DEVICE};
    static final int[] ADAPTER_TO = {R.id.tab_icon, R.id.tab_title, R.id.tab_link, R.id.tab_date, R.id.device};

    private final SimpleCursorAdapter.ViewBinder ROW_BINDER = new SharedTabsBinder(this);

    ListView sharedTabs;
    SimpleCursorAdapter sharedTabsAdapter;

    private DbHelper dbHelper;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DbHelper(this);

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
            refreshSharedTabs();

            // if not authenticated - then no cache
            // because we cleanup cache on logout
            new CleanupCacheTask().execute();
        }
    }

    @Override
    protected void onDestroy() {
        super.onStop();

        dbHelper.close();
    }

    private boolean refreshAdapter() {
        final Cursor cursor = dbHelper.findSharedTabs();
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

        return (cursor.getCount() != 0);
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

            menu.add(Menu.NONE, TAB_CONTEXT_MENU_COPY, TAB_CONTEXT_MENU_COPY,
                    getResources().getString(R.string.copy_link));
        }
    }

    private void setTabContextMenuHeader(ListView listView, ContextMenu menu,
                                         AdapterView.AdapterContextMenuInfo ctxMenuInfo) {

        final Cursor cursor = (Cursor) listView.getAdapter().getItem(ctxMenuInfo.position);

        final int titleColumn = cursor.getColumnIndex(DbMetadata.TITLE);
        final String title = cursor.getString(titleColumn);

        if (title != null && title.length() != 0) {
            menu.setHeaderTitle(Html.fromHtml(title).toString());
        }
        else {
            final int linkColumn = cursor.getColumnIndex(DbMetadata.LINK);
            String link = cursor.getString(linkColumn);
            link = UrlUtil.decodeLink(link);
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
                sendLink(cursor);
                break;
            case TAB_CONTEXT_MENU_COPY:
                copyLink(cursor);
                break;
        }

        return true;
    }

    private void sendLink(Cursor cursor) {
        final int linkColumn = cursor.getColumnIndex(DbMetadata.LINK);
        final int titleColumn = cursor.getColumnIndex(DbMetadata.TITLE);

        final String link = cursor.getString(linkColumn);
        final String title = cursor.getString(titleColumn);

        new SendTabTask().execute(link, title);
    }

    private void copyLink(Cursor cursor) {
        final int linkColumn = cursor.getColumnIndex(DbMetadata.LINK);
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
        switch (item.getItemId()) {
            case R.id.refresh:
                refreshSharedTabs();
                break;
            case R.id.logout:
                getSyncTabApplication().logout();
                showLogin();
                break;
        }
        return true;
    }

    public void refreshSharedTabs() {
        boolean filled = refreshAdapter();
        new RefreshSharedTabsTask(filled).execute();
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
        }

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
                refreshSharedTabs();
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

            String chooserTitle = getResources().getString(R.string.send_to);
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
                SyncTabRemoteService service = getSyncTabApplication().getSyncTabRemoteService();
                return !service.getOlderSharedTabs();
            }
            catch (Exception e) {
                Log.e(MainActivity.TAG, "error to load older shared tabs", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean empty) {
            super.onPostExecute(empty);
            if (!empty) {
                refreshAdapter();
            }
        }
    }

    private static class SharedTabsBinder implements SimpleCursorAdapter.ViewBinder {

        private final Context context;
        private final CacheManager cacheManager;

        private SharedTabsBinder(Context context) {
            this.context = context;
            this.cacheManager = new CacheManager(context);
        }

        public boolean setViewValue(View element, Cursor cursor, int columnIndex) {
            if (element.getId() == R.id.tab_icon) {
                String favicon = cursor.getString(columnIndex);
                if (favicon != null && favicon.length() > 0) {
                    InputStream image = cacheManager.read(favicon);
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

                final String deviceName;
                if (AppConstants.ANDROID_SYNCTAB_DEVICE.equals(device)) {
                    deviceName = "Android";
                }
                else {
                    deviceName = context.getResources().getString(R.string.unknown);
                }
                ((TextView) element).setText(deviceName);

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
                    int maxlength = context.getResources().getInteger(R.integer.link_max_size);
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