package com.khmlabs.synctab.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.SyncTabRemoteService;
import com.khmlabs.synctab.db.DbHelper;
import com.khmlabs.synctab.queue.QueueTask;

import java.util.List;

public class SyncTabService extends Service {

    private boolean running;
    private Synchronizer synchronizer;
    private DbHelper dbHelper;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        running = false;
        dbHelper = new DbHelper(this);
        synchronizer = new Synchronizer(dbHelper);
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

        dbHelper.close();
        dbHelper = null;

        running = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        final SyncTabApplication app = (SyncTabApplication) getApplication();

        if (!running && app.isOnLine()) {
            running = true;
            synchronizer.start();
        }

        return START_STICKY;
    }

    class Synchronizer extends Thread {

        private DbHelper dbHelper;

        Synchronizer(DbHelper dbHelper) {
            super("SynchronizerThread");

            this.dbHelper = dbHelper;
        }

        public void run() {
            try {
                final SyncTabApplication app = (SyncTabApplication) getApplication();
                final SyncTabRemoteService service = app.getSyncTabRemoteService();
                final List<QueueTask> tasks = dbHelper.getQueuedTasks();

                if (isInterrupted()) {
                    return;
                }

                for (QueueTask task : tasks) {
                    if (isInterrupted()) {
                        return;
                    }

                    if (service.syncTask(task)) {
                        dbHelper.removeQueueTask(task);
                    }
                }
            }
            finally {
                dbHelper.close();
                SyncTabService.this.stopSelf();
            }

        }

    }
}
