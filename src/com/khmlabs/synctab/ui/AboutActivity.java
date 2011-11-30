package com.khmlabs.synctab.ui;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import com.khmlabs.synctab.R;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.util.AppHelper;
import com.khmlabs.synctab.util.IntentHelper;

/**
 * Shows some detail information about user and application.
 */
public class AboutActivity extends PreferenceUserActivity {

    private static final int USERNAME = 0;
    private static final int VERSION = 1;
    private static final int LINKS_CATEGORY = 2;

    private static final int[] BROWSABLE_URLS = {
            R.string.website_url,
            R.string.chrome_extension_url,
            R.string.help_url,
            R.string.submit_issue_url,
            R.string.contact_url
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about);
        setContentView(R.layout.activity_about);

        initInformation();
    }

    private void initInformation() {
        final SyncTabApplication app = (SyncTabApplication) getApplication();

        final PreferenceScreen screen = getPreferenceScreen();

        // -- Username preparation

        Preference username = screen.getPreference(USERNAME);
        if (app.isAuthenticated()) {
            username.setSummary(app.getAuthEmail());
        }

        // -- Version preparation

        Preference version = screen.getPreference(VERSION);
        version.setSummary(AppHelper.getVersionName(this));

        // -- Commands

        PreferenceCategory category = (PreferenceCategory) screen.getPreference(LINKS_CATEGORY);

        for (int i = 0; i < BROWSABLE_URLS.length; i++) {
            final String url = getResources().getString(BROWSABLE_URLS[i]);

            category.getPreference(i).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    IntentHelper.browseLink(AboutActivity.this, url);
                    return true;
                }
            });
        }
    }

}
