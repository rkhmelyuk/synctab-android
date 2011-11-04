package com.khmlabs.synctab.queue;

import android.util.Log;

import com.khmlabs.synctab.RemoteOpStatus;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.db.SyncTabDatabase;

public class TaskQueueManager {

    private static final String TAG = "TaskQueueManager";

    private final SyncTabApplication application;

    public TaskQueueManager(SyncTabApplication application) {
        this.application = application;
    }

    public RemoteOpStatus addShareTabTask(String link) {
        return addTask(new QueueTask(TaskType.SyncTab, link));
    }

    public RemoteOpStatus addLogoutTask(String token) {
        return addTask(new QueueTask(TaskType.Logout, token));
    }

    public RemoteOpStatus addRemoveTabTask(String sharedTabId) {
        return addTask(new QueueTask(TaskType.RemoveSharedTab, sharedTabId));
    }

    public RemoteOpStatus addReshareTabTask(String sharedTabId) {
        return addTask(new QueueTask(TaskType.ReshareTab, sharedTabId));
    }

    public RemoteOpStatus addLoadFaviconTask(String favicon) {
        return addTask(new QueueTask(TaskType.LoadFavicon, favicon));
    }

    private RemoteOpStatus addTask(QueueTask task) {
        SyncTabDatabase database = null;
        try {
            database = new SyncTabDatabase(application);
            database.insertQueueTask(task);

            Log.i(TAG, "Added task to QUEUE: " + task.getType() + " -> " + task.getParam());

            return RemoteOpStatus.Queued;
        }
        catch (Exception e) {
            Log.e(TAG, "Error to add sync tab task to queue", e);
        }
        finally {
            if (database != null) {
                database.close();
            }
        }

        return RemoteOpStatus.Failed;
    }
}
