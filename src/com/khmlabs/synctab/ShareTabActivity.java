package com.khmlabs.synctab;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class ShareTabActivity extends BaseActivity {

    private static final String TAG = "ShareTabActivity";

    private TextView linkText;
    private TextView statusText;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.synctab);

        // show the link in the text view
        statusText = (TextView) findViewById(R.id.sync_tab_status);
        statusText.setText(R.string.enqueue_sync);

        linkText = (TextView) findViewById(R.id.sync_tab_link);
    }

    @Override
    protected void onResume() {
        super.onResume();

        shareLinkIfAuthenticated();
    }

    private void shareLinkIfAuthenticated() {
        if (getSyncTabApplication().isAuthenticated()) {
            // get the shared link
            final String link = getIntent().getStringExtra(Intent.EXTRA_TEXT);

            linkText.setText(link);
            new SyncTabTask().execute(link);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sharedtab_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view_shared_tabs:
                final Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
                break;
            case R.id.logout:
                getSyncTabApplication().logout();
                showLogin();
                break;
        }

        return true;
    }

    class SyncTabTask extends AsyncTask<String, String, Boolean> {

        protected Boolean doInBackground(String... strings) {
            final String link = strings[0];
            Log.i(TAG, "Sharing link " + link);

            SyncTabApplication application = (SyncTabApplication) getApplication();
            SyncTabRemoteService service = application.getSyncTabRemoteService();
            return service.enqueueSync(link) != RemoteOpState.Failed;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (result) {
                statusText.setText(R.string.success_sync_queue);
            }
            else {
                statusText.setText(R.string.error_sync_queue);
            }
        }
    }

}