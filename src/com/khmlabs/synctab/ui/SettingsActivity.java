package com.khmlabs.synctab.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.khmlabs.synctab.R;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.tag.Tag;

import java.util.List;

/**
 * @author Ruslan Khmelyuk
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int CURRENT_TAG_INDEX = 0;
    private static final int REFRESH_PERIOD_INDEX = 1;

    TitleBarHelper titlebarHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        setContentView(R.layout.activity_settings);

        titlebarHelper = new TitleBarHelper(this);

        initPreferences();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);
    }

    protected void onResume() {
        super.onResume();

        // add any buttons to title bar
        titlebarHelper.setup();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        initPreferences();
    }

    private void initPreferences() {
        final SyncTabApplication app = (SyncTabApplication) getApplication();
        final List<Tag> tags = app.getFacade().getTags();

        final String[] tagIds = new String[tags.size()];
        final String[] tagNames = new String[tags.size()];

        int i = 0;
        for (Tag each : tags) {
            tagIds[i] = each.getTagId();
            tagNames[i] = each.getName();

            i++;
        }

        final PreferenceScreen screen = getPreferenceScreen();

        // -- Current Tag preference preparation

        ListPreference tagPref = (ListPreference) screen.getPreference(CURRENT_TAG_INDEX);
        tagPref.setEntries(tagNames);
        tagPref.setEntryValues(tagIds);

        Tag tag = app.getFacade().getTag(app.getCurrentTag());
        if (tag != null) {
            tagPref.setSummary(tag.getName());
        }

        // -- Refresh Period preference preparation

        ListPreference refreshPref = (ListPreference) screen.getPreference(REFRESH_PERIOD_INDEX);
        int minutes = (int) (app.getRefreshPeriod() / 60000);
        String message = getResources().getQuantityString(R.plurals.minutes, minutes, minutes);
        refreshPref.setSummary(message);
    }


}
