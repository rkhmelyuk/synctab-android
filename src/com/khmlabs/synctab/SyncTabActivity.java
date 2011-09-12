package com.khmlabs.synctab;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

public class SyncTabActivity extends Activity {

    private TextView statusText;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.synctab);

        // get the shared link
        final String link = getIntent().getStringExtra(Intent.EXTRA_TEXT);

        syncLink(link);

        // show the link in the text view
        statusText = (TextView) findViewById(R.id.sync_tab_status);
        statusText.setText(R.string.enqueue_sync);

        final TextView linkText = (TextView) findViewById(R.id.sync_tab_link);
        linkText.setText(link);
    }

    private void syncLink(String link) {
        new SyncTabTask().execute(link);
    }

    class SyncTabTask extends AsyncTask<String, String, Boolean> {

        protected Boolean doInBackground(String... strings) {
            // enqueue the link

            SyncTabApplication application = (SyncTabApplication) getApplication();
            SyncTabRemoteService service = application.getSyncTabService();
            return service.enqueueSync(strings[0]);
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