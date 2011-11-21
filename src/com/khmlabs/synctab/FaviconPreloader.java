package com.khmlabs.synctab;

import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.khmlabs.synctab.tab.SharedTab;

import java.util.List;

/**
 * Preloader of favicons.
 *
 * @author Ruslan Khmelyuk
 */
public class FaviconPreloader {

    private static final String TAG = "FaviconPreloader";

    private final FileCacheManager cacheManager;
    private final SyncTabApplication application;

    public FaviconPreloader(SyncTabApplication application) {
        this.application = application;
        this.cacheManager = application.getCacheManager();
    }

    /**
     * Preload and cache icons for each tab in the a list.
     *
     * @param tabs the list of tabs to preload icons for.
     */
    public void preloadForTabs(List<SharedTab> tabs) {
        new Thread(new Preloader(tabs)).start();
    }

    /**
     * Preload the icon by url.
     *
     * @param url the icon url.
     * @return true if icon was preloaded and cached.
     */
    public boolean preloadFavicon(String url) {
        return downloadAndCacheFavicon(url);
    }

    private boolean downloadAndCacheFavicon(String url) {
        try {
            final HttpClient client = new DefaultHttpClient();
            final HttpResponse response = client.execute(new HttpGet(url));

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // cache the icon if response code was success
                return cacheManager.store(url, response.getEntity().getContent());
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

                    // avoid preloading the same icon a few times
                    if (cacheManager.containsKey(favicon)) {
                        continue;
                    }

                    if (application.isOnLine()) {
                        // download and cache the favicon
                        downloadAndCacheFavicon(favicon);
                    }
                    else {
                        // if not online, than add to the task queue
                        application.getTaskQueueManager().addLoadFaviconTask(favicon);
                    }
                }
            }
        }
    }
}
