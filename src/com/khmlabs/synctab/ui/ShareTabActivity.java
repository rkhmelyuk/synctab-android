package com.khmlabs.synctab.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

import com.khmlabs.synctab.*;
import com.khmlabs.synctab.tag.Tag;
import com.khmlabs.synctab.util.UrlUtil;

import java.util.List;

/**
 * Activity to share a tab.
 */
public class ShareTabActivity extends BaseUserActivity {

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
        final SyncTabApplication app = getSyncTabApplication();

        if (app.isAuthenticated()) {
            showShareTags(app);
        }
    }

    private void showShareTags(SyncTabApplication app) {
        final SyncTabFacade facade = app.getFacade();
        final List<Tag> tags = facade.getShareTags();
        final String[] tagsArray = tagsListToNameArray(tags);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.send_tab_to));
        builder.setItems(tagsArray, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int position) {
                final Tag tag = tags.get(position);
                shareLink(tag);
            }
        });

        AlertDialog tagsDialog = builder.create();

        // close activity on cancel tags selecting,
        // as it is required to select at least one tag
        tagsDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialogInterface) {
                ShareTabActivity.this.finish();
            }
        });

        tagsDialog.show();
    }

    private String[] tagsListToNameArray(List<Tag> tags) {
        String[] result = new String[tags.size()];

        int i = 0;
        for (Tag each : tags) {
            result[i++] = each.getName();
        }

        return result;
    }

    private void shareLink(Tag tag) {
        // get the shared link
        final Intent intent = getIntent();
        String link = intent.getStringExtra(Intent.EXTRA_TEXT);

        if (link != null && link.length() > 0) {
            link = UrlUtil.decodeUrl(link);
            if (URLUtil.isValidUrl(link)) {
                String tagId = null;
                if (tag != null) {
                    tagId = tag.getTagId();
                }

                new SyncTabTask().execute(link, tagId);
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

    class SyncTabTask extends AsyncTask<String, String, RemoteOpStatus> {

        protected RemoteOpStatus doInBackground(String... strings) {
            final String link = strings[0];
            final String tagId = strings[1];
            if (AppConstants.LOG) Log.i(TAG, "Sharing link " + link);

            SyncTabApplication application = (SyncTabApplication) getApplication();
            SyncTabFacade facade = application.getFacade();
            return facade.enqueueSync(link, tagId);
        }

        @Override
        protected void onPostExecute(RemoteOpStatus result) {
            super.onPostExecute(result);

            if (result == RemoteOpStatus.Success) {
                statusImage.setImageResource(R.drawable.yes);
                statusText.setText(R.string.success_sync);
            }
            else if (result == RemoteOpStatus.Queued) {
                statusImage.setImageResource(R.drawable.yes);
                statusText.setText(R.string.enqueue_sync);
            }
            else if (result == RemoteOpStatus.Failed) {
                statusImage.setImageResource(R.drawable.fail);
                statusText.setText(R.string.failed_sync);
            }
        }
    }

}