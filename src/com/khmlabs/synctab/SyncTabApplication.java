package com.khmlabs.synctab;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

import java.net.URL;

public class SyncTabApplication extends Application {

    private static final String TAG = "SyncTabApplication";

    private SyncTabRemoteService syncTabRemoteService;
    private boolean onLine = false;

    @Override
    public void onCreate() {
        super.onCreate();

        //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        //preferences.registerOnSharedPreferenceChangeListener(this);
        initSyncTabRemoteService();

        ConnectivityManager connectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        onLine = connectionManager.getActiveNetworkInfo() != null && connectionManager.getActiveNetworkInfo().isConnected();
    }

    private synchronized void initSyncTabRemoteService() {

        try {
            final URL url = new URL(AppConstants.SERVICE_URL);
            syncTabRemoteService = new SyncTabRemoteService(
                    this, url.getProtocol(),
                    url.getHost(), getPort(url, 80));
        }
        catch (Exception e) {
            Log.e(TAG, "Error to connect to SyncTab Service.", e);
        }
    }

    private int getPort(URL url, int defaultPort) {
        int port = url.getPort();
        return (port != -1 ? port : defaultPort);
    }

    public SyncTabRemoteService getSyncTabRemoteService() {
        return syncTabRemoteService;
    }

    public boolean isOnLine() {
        return onLine;
    }

    public void setOnLine(boolean onLine) {
        this.onLine = onLine;
    }
}
