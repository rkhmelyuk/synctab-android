package com.khmlabs.synctab.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.SyncTabFacade;
import com.khmlabs.synctab.tab.SharedTab;
import com.khmlabs.synctab.util.IntentHelper;

import java.util.List;

/**
 * This service is responsible for getting the list of tabs and tags.
 *
 * @author Ruslan Khmelyuk
 */
public class RefreshService extends Service {

    private static final String TAG = "RefreshService";

    private boolean running;
    private Refresher refresher;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        running = false;
        refresher = new Refresher();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        refresher.interrupt();
        try {
            refresher.join();
        }
        catch (Exception e) {
            // ignore
        }
        refresher = null;

        running = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        final SyncTabApplication app = (SyncTabApplication) getApplication();

        if (!running && app.isOnLine() && app.isAuthenticated()) {
            running = true;
            refresher.start();
        }

        return START_STICKY;
    }

    class Refresher extends Thread {

        Refresher() {
            super("RefresherThread");
        }

        public void run() {
            final SyncTabApplication app = (SyncTabApplication) getApplication();
            final SyncTabFacade facade = app.getFacade();

            try {
                while (!isInterrupted()) {
                    // Refresh the list of tags.
                    facade.refreshTags();

                    // Receive & open tabs
                    List<SharedTab> tabs = facade.receiveSharedTabs();
                    for (SharedTab each : tabs) {
                        IntentHelper.browseLink(RefreshService.this, each.getLink());
                    }

                    Thread.sleep(app.getRefreshPeriod());
                }
            }
            catch (InterruptedException e) {
                // nothing, it was just interrupted by stopping service
            }
            catch (Exception e) {
                Log.e(TAG, "Error to refresh", e);
            }
        }
    }

}
