package com.khmlabs.synctab;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.net.URL;

public class SyncTabApplication extends Application {

    private static final String TAG = "SyncTabApplication";

    private volatile boolean onLine = false;

    private SharedPreferences preferences;
    private SyncTabRemoteService syncTabRemoteService;

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        setOnlineStatus();

        initSyncTabRemoteService();
    }

    private void setOnlineStatus() {
        ConnectivityManager connectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectionManager.getActiveNetworkInfo();
        onLine =  networkInfo != null && connectionManager.getActiveNetworkInfo().isConnected();
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
        final String token = getAuthToken();

        setAuthToken(null);
        setAuthEmail(null);
        setLastSyncTime(0);
        setLastSharedTabId(null);

        syncTabRemoteService.removeUserData();

        if (onLine) {
            syncTabRemoteService.logout(token);
        }

        Log.i(TAG, "Logout");
    }

    public boolean isAuthenticated() {
        return getAuthToken() != null;
    }

    public long getLastSyncTime() {
        return preferences.getLong(AppConstants.LAST_SYNC_TIME, 0);
    }

    public void setLastSyncTime(long timestamp) {
        preferences.edit().putLong(AppConstants.LAST_SYNC_TIME, timestamp).commit();
    }

    public String getLastSharedTabId() {
        return preferences.getString(AppConstants.LAST_SHARED_TAB_ID, null);
    }

    public void setLastSharedTabId(String id) {
        preferences.edit().putString(AppConstants.LAST_SHARED_TAB_ID, id).commit();
    }
}
