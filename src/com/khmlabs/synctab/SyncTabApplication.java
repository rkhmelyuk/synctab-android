package com.khmlabs.synctab;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.khmlabs.synctab.queue.TaskQueueManager;

import java.net.URL;

public class SyncTabApplication extends Application {

    private static final String TAG = "SyncTabApplication";

    /**
     * The flag used to check if application is online.
     * TODO - maybe need to use information from ConnectivityManager each time.
     */
    private volatile boolean onLine = false;

    private SharedPreferences preferences;
    private SyncTabService syncTabService;
    private FileCacheManager cacheManager;
    private TaskQueueManager taskQueueManager;

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        setOnlineStatus();

        cacheManager = new FileCacheManager(this);
        taskQueueManager = new TaskQueueManager(this);

        initSyncTabRemoteService();
    }

    public void cleanupCacheIfNeed() {

        final long now = System.currentTimeMillis();
        final long time = getLastCacheCleanupTime();

        if (time > 0 && (now - time > AppConstants.CACHE_CLEANUP_PERIOD)) {
            if (cacheManager.isNeedCleanup()) {
                cacheManager.clean();
            }
        }
        setLastCacheCleanupTime(now);
    }

    private void setOnlineStatus() {
        ConnectivityManager connectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectionManager.getActiveNetworkInfo();
        onLine =  networkInfo != null && connectionManager.getActiveNetworkInfo().isConnected();
    }

    private synchronized void initSyncTabRemoteService() {
        try {
            final URL url = new URL(AppConstants.SERVICE_URL);
            syncTabService = new SyncTabService(
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

    public SyncTabService getSyncTabService() {
        return syncTabService;
    }

    public FileCacheManager getCacheManager() {
        return cacheManager;
    }

    public TaskQueueManager getTaskQueueManager() {
        return taskQueueManager;
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

    // TODO - do something with this, not the best place for it
    public void logout() {
        final String token = getAuthToken();

        setAuthToken(null);
        setAuthEmail(null);
        setLastSyncTime(0);
        setLastSharedTabId(null);

        cacheManager.clean();
        syncTabService.removeUserData();

        if (onLine) {
            syncTabService.logout(token);
        }

        if (AppConstants.LOG) Log.i(TAG, "Logout");
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

    public long getLastCacheCleanupTime() {
        return preferences.getLong(AppConstants.LAST_CACHE_CLEANUP_TIME, 0);
    }

    public void setLastCacheCleanupTime(long timestamp) {
        preferences.edit().putLong(AppConstants.LAST_CACHE_CLEANUP_TIME, timestamp).commit();
    }

    public String getLastSharedTabId() {
        return preferences.getString(AppConstants.LAST_SHARED_TAB_ID, null);
    }

    public void setLastSharedTabId(String id) {
        preferences.edit().putString(AppConstants.LAST_SHARED_TAB_ID, id).commit();
    }

    public String getOldestSharedTabId() {
        return preferences.getString(AppConstants.OLDEST_SHARED_TAB_ID, null);
    }

    public void setOldestSharedTabId(String id) {
        preferences.edit().putString(AppConstants.OLDEST_SHARED_TAB_ID, id).commit();
    }
}
