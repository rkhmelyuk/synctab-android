package com.khmlabs.synctab;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SyncTabApplication extends Application {

    private SyncTabRemoteService syncTabService;
    private boolean onLine = false;

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        //preferences.registerOnSharedPreferenceChangeListener(this);

        syncTabService = new SyncTabRemoteService();
    }

    public SyncTabRemoteService getSyncTabService() {
        return syncTabService;
    }

    public boolean isOnLine() {
        return onLine;
    }

    public void setOnLine(boolean onLine) {
        this.onLine = onLine;
    }
}
