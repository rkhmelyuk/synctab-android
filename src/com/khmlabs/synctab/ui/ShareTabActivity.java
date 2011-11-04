package com.khmlabs.synctab.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.TextView;

import com.khmlabs.synctab.R;
import com.khmlabs.synctab.RemoteOpState;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.SyncTabService;
import com.khmlabs.synctab.util.UrlUtil;

public class ShareTabActivity extends BaseActivity {

    private static final String TAG = "ShareTabActivity";

    TextView statusText;
    ImageView statusImage;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sharetab);

        // show the link in the text view
        statusText = (TextView) findViewById(R.id.sync_tab_status);
        statusText.setText(R.string.sync_in_progress);

        statusImage = (ImageView) findViewById(R.id.share_tab_status_img);
    }

    @Override
    protected void onResume() {
        super.onResume();

        shareLinkIfAuthenticated();
    }

    private void shareLinkIfAuthenticated() {
        if (getSyncTabApplication().isAuthenticated()) {
            // get the shared link
            final Intent intent = getIntent();
            String link = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (link != null && link.length() > 0) {
                link = UrlUtil.decodeUrl(link);
                if (URLUtil.isValidUrl(link)) {
                    new SyncTabTask().execute(link);
                }
                else {
                    statusImage.setImageResource(R.drawable.fail);
                    statusText.setText(R.string.incorrect_url);
                }
            }
            else {
                statusImage.setImageResource(R.drawable.fail);
                statusText.setText(R.string.nothing_to_share);
            }
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
        boolean result = super.onOptionsItemSelected(item);
        if (!result) {
            switch (item.getItemId()) {
                case R.id.view_shared_tabs:
                    final Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    finish();
                    return true;
            }
        }
        return result;
    }

    class SyncTabTask extends AsyncTask<String, String, RemoteOpState> {

        protected RemoteOpState doInBackground(String... strings) {
            final String link = strings[0];
            Log.i(TAG, "Sharing link " + link);

            SyncTabApplication application = (SyncTabApplication) getApplication();
            SyncTabService service = application.getSyncTabService();
            return service.enqueueSync(link);
        }

        @Override
        protected void onPostExecute(RemoteOpState result) {
            super.onPostExecute(result);

            if (result == RemoteOpState.Success) {
                statusImage.setImageResource(R.drawable.yes);
                statusText.setText(R.string.success_sync);
            }
            else if (result == RemoteOpState.Queued) {
                statusImage.setImageResource(R.drawable.yes);
                statusText.setText(R.string.enqueue_sync);
            }
            else if (result == RemoteOpState.Failed) {
                statusImage.setImageResource(R.drawable.fail);
                statusText.setText(R.string.failed_sync);
            }
        }
    }

}