package com.khmlabs.synctab.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.khmlabs.synctab.R;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.util.AppHelper;

/**
 * Shows some detail information about user and application.
 */
public class AboutActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // ---- username if authenticated
        final SyncTabApplication app = getSyncTabApplication();
        if (app.isAuthenticated()) {
            // set user email as username
            final TextView username = (TextView) findViewById(R.id.username);
            username.setText(getSyncTabApplication().getAuthEmail());
        }
        else {
            // hide the panel with user info
            findViewById(R.id.user_info).setVisibility(View.GONE);
        }

        // ---- application version
        final TextView version = (TextView) findViewById(R.id.version);
        version.setText(AppHelper.getVersionName(this));
    }
}
