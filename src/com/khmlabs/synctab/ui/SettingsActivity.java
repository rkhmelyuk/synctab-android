package com.khmlabs.synctab.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import com.khmlabs.synctab.R;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.tag.Tag;
import com.khmlabs.synctab.util.IntentHelper;
import com.khmlabs.synctab.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity to edit settings.
 */
public class SettingsActivity extends PreferenceUserActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int CURRENT_TAG_INDEX = 0;
    private static final int EDIT_TAGS_INDEX = 1;
    private static final int REFRESH_PERIOD_INDEX = 2;

    boolean refreshPreference = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        setContentView(R.layout.activity_settings);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshPreference = true;

        initPreferences();
    }

    @Override
    protected void onPause() {
        super.onPause();

        refreshPreference = false;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (refreshPreference) {
            initPreferences();
        }
    }

    private void initPreferences() {
        final SyncTabApplication app = getSyncTabApplication();
        final List<Tag> tags = app.getFacade().getTags();

        final List<String> tagIds = new ArrayList<String>();
        final List<String> tagNames = new ArrayList<String>();

        fillTagsEntries(tags, tagIds, tagNames);

        final PreferenceScreen screen = getPreferenceScreen();

        // -- Current Tag preference preparation

        ListPreference tagPref = (ListPreference) screen.getPreference(CURRENT_TAG_INDEX);

        int size = tagNames.size();
        tagPref.setEntries(tagNames.toArray(new String[size]));
        tagPref.setEntryValues(tagIds.toArray(new String[size]));

        Tag tag = app.getFacade().getTag(app.getCurrentTag());
        if (tag != null && tag.getName() != null) {
            tagPref.setSummary(tag.getName());
        }

        // -- Edit Tags shows a Tag Edit Activity

        Preference tagEditPref = screen.getPreference(EDIT_TAGS_INDEX);
        tagEditPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                IntentHelper.showTagEditActivity(SettingsActivity.this);
                return true;
            }
        });

        // -- Refresh Period preference preparation

        Preference refreshPref = screen.getPreference(REFRESH_PERIOD_INDEX);
        int minutes = (int) (app.getRefreshPeriod() / 60000);
        String message = getResources().getQuantityString(R.plurals.minutes, minutes, minutes);
        refreshPref.setSummary(message);
    }

    /**
     * Fill tags list entries: labels and values.
     * Also filters the list of tags to include only synced remove tags.
     *
     * @param tags the list of all tags.
     * @param tagIds the tags entries values.
     * @param tagNames the tags entries labels.
     */
    private void fillTagsEntries(List<Tag> tags, List<String> tagIds, List<String> tagNames) {
        for (Tag each : tags) {
            // only add non-local tags
            if (StringUtil.isNotEmpty(each.getTagId())) {
                tagIds.add(each.getTagId());
                tagNames.add(each.getName());
            }
        }
    }

}
