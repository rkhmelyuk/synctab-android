package com.khmlabs.synctab;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.net.URL;

public class SyncTabApplication extends Application {

    private static final String TAG = "SyncTabApplication";

    private SharedPreferences preferences;
    private SyncTabRemoteService syncTabRemoteService;
    private boolean onLine = false;

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
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

    public String getAuthEmail() {
        return preferences.getString(AppConstants.AUTH_USER, null);
    }

    public void setAuthEmail(String email) {
        preferences.edit().putString(AppConstants.AUTH_USER, email).commit();
    }

    public String getAuthToken() {
        return preferences.getString(AppConstants.AUTH_TOKEN, null);
    }

    public void setAuthToken(String token) {
        preferences.edit().putString(AppConstants.AUTH_TOKEN, token).commit();
    }

    public void logout() {
        setAuthToken(null);
        Log.i(TAG, "Logout");
    }

    public boolean isAuthenticated() {
        return getAuthToken() != null;
    }
}
