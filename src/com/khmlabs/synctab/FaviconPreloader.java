package com.khmlabs.synctab;

import android.util.Log;
import com.khmlabs.synctab.tab.SharedTab;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.List;

public class FaviconPreloader {

    private static final String TAG = "FaviconPreloader";

    private final SyncTabApplication application;

    public FaviconPreloader(SyncTabApplication application) {
        this.application = application;
    }

    public void preloadForTabs(List<SharedTab> tabs) {
        new Thread(new Preloader(tabs)).start();
    }

    private class Preloader implements Runnable {

        final List<SharedTab> tabs;
        final CacheManager cacheManager;

        private Preloader(List<SharedTab> tabs) {
            this.tabs = tabs;
            this.cacheManager = application.getCacheManager();
        }

        public void run() {
            for (final SharedTab each : tabs) {
                final String favicon = each.getFavicon();
                if (favicon != null && favicon.length() > 0) {
                    if (application.isOnLine()) {
                        downloadAndCacheFavicon(favicon);
                    }
                    else {
                        // TODO - enqueue to download when online
                    }
                }
            }
        }

        private void downloadAndCacheFavicon(String url) {
            try {
                final HttpClient client = new DefaultHttpClient();
                final HttpResponse response = client.execute(new HttpGet(url));
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    cacheManager.store(url, response.getEntity().getContent());
                }
            }
            catch (IOException e) {
                Log.w(TAG, "Error to download favicon " + url);
            }
        }
    }
}
