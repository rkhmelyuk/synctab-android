package com.khmlabs.synctab.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.khmlabs.synctab.SyncTabApplication;

/**
 * Base preference activity for user.
 */
public abstract class PreferenceUserActivity extends PreferenceActivity {

    TitleBarHelper titlebarHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        titlebarHelper = new TitleBarHelper(this);
    }

    protected void onResume() {
        super.onResume();

        // add any buttons to title bar
        titlebarHelper.setup();
    }

    protected SyncTabApplication getSyncTabApplication() {
        return (SyncTabApplication) getApplication();
    }
}
