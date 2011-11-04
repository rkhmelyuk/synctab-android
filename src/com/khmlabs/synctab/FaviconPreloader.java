package com.khmlabs.synctab;

import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.khmlabs.synctab.tab.SharedTab;

import java.util.List;

public class FaviconPreloader {

    private static final String TAG = "FaviconPreloader";

    private final CacheManager cacheManager;
    private final SyncTabApplication application;

    public FaviconPreloader(SyncTabApplication application) {
        this.application = application;
        this.cacheManager = application.getCacheManager();
    }

    public void preloadForTabs(List<SharedTab> tabs) {
        new Thread(new Preloader(tabs)).start();
    }

    public boolean preloadFavicon(String url) {
        return downloadAndCacheFavicon(url);
    }

    private boolean downloadAndCacheFavicon(String url) {
        try {
            final HttpClient client = new DefaultHttpClient();
            final HttpResponse response = client.execute(new HttpGet(url));
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                cacheManager.store(url, response.getEntity().getContent());
                return true;
            }
        }
        catch (Exception e) {
            Log.w(TAG, "Error to download favicon " + url);
        }

        return false;
    }

    private class Preloader implements Runnable {

        final List<SharedTab> tabs;

        private Preloader(List<SharedTab> tabs) {
            this.tabs = tabs;
        }

        public void run() {
            for (final SharedTab each : tabs) {
                final String favicon = each.getFavicon();
                if (favicon != null && favicon.length() > 0) {
                    if (application.isOnLine()) {
                        downloadAndCacheFavicon(favicon);
                    }
                    else {
                        application.getTaskQueueManager().addLoadFaviconTask(favicon);
                    }
                }
            }
        }
    }
}
