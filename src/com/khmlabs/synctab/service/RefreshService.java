package com.khmlabs.synctab.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.SyncTabFacade;
import com.khmlabs.synctab.db.SyncTabDatabase;
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

    private static final long CHECK_PERIOD = 60000L;

    private boolean running;
    private Refresher refresher;
    private SyncTabDatabase database;

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

        database.close();
        database = null;

        running = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        final SyncTabApplication app = (SyncTabApplication) getApplication();

        if (!running && app.isOnLine() && app.isAuthenticated()) {
            System.out.println("STARTED REFRESH_SERVICE");

            running = true;
            refresher.start();
        }
        else {
            System.out.println("no starting " + running + " " + app.isOnLine() + " " + app.isAuthenticated());
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

                    Thread.sleep(CHECK_PERIOD);
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Error to refresh", e);
            }
        }
    }

}
