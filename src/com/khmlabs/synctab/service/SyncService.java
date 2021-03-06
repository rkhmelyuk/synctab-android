package com.khmlabs.synctab.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.SyncTabFacade;
import com.khmlabs.synctab.db.SyncTabDatabase;
import com.khmlabs.synctab.queue.QueueTask;

import java.util.List;

public class SyncService extends Service {

    private boolean running;
    private Synchronizer synchronizer;
    private SyncTabDatabase database;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        running = false;
        database = new SyncTabDatabase(this);
        synchronizer = new Synchronizer(database);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        synchronizer.interrupt();
        try {
            synchronizer.join();
        }
        catch (Exception e) {
            // ignore
        }
        synchronizer = null;

        database.close();
        database = null;

        running = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        final SyncTabApplication app = (SyncTabApplication) getApplication();

        if (!running && app.isOnLine()) {
            running = true;
            synchronizer.start();

            return START_STICKY;
        }

        return START_NOT_STICKY;
    }

    class Synchronizer extends Thread {

        private SyncTabDatabase database;

        Synchronizer(SyncTabDatabase database) {
            super("SynchronizerThread");

            this.database = database;
        }

        public void run() {
            try {
                final SyncTabApplication app = (SyncTabApplication) getApplication();
                final SyncTabFacade facade = app.getFacade();

                // Refresh the list of tags.
                facade.refreshTags();
                if (isInterrupted()) return;

                final List<QueueTask> tasks = database.getQueuedTasks();

                if (isInterrupted()) return;

                for (QueueTask task : tasks) {
                    if (isInterrupted()) {
                        return;
                    }

                    if (facade.syncTask(task)) {
                        database.removeQueueTask(task);
                    }
                }
            }
            finally {
                database.close();
                SyncService.this.stopSelf();
            }
        }
    }
}
