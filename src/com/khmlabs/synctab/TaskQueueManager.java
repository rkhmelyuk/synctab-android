package com.khmlabs.synctab;

import android.util.Log;

import com.khmlabs.synctab.db.SyncTabDatabase;
import com.khmlabs.synctab.queue.QueueTask;
import com.khmlabs.synctab.queue.TaskType;

public class TaskQueueManager {

    private static final String TAG = "TaskQueueManager";

    private final SyncTabApplication application;

    public TaskQueueManager(SyncTabApplication application) {
        this.application = application;
    }

    public RemoteOpState addShareTabTask(String link) {
        return addTask(new QueueTask(TaskType.SyncTab, link));
    }

    public RemoteOpState addLogoutTask(String token) {
        return addTask(new QueueTask(TaskType.Logout, token));
    }

    public RemoteOpState addRemoveTabTask(String sharedTabId) {
        return addTask(new QueueTask(TaskType.RemoveSharedTab, sharedTabId));
    }

    public RemoteOpState addReshareTabTask(String sharedTabId) {
        return addTask(new QueueTask(TaskType.ReshareTab, sharedTabId));
    }

    public RemoteOpState addLoadFaviconTask(String favicon) {
        return addTask(new QueueTask(TaskType.LoadFavicon, favicon));
    }

    private RemoteOpState addTask(QueueTask task) {
        SyncTabDatabase database = null;
        try {
            database = new SyncTabDatabase(application);
            database.insertQueueTask(task);

            Log.i(TAG, "Added task to QUEUE: " + task.getType() + " -> " + task.getParam());

            return RemoteOpState.Queued;
        }
        catch (Exception e) {
            Log.e(TAG, "Error to add sync tab task to queue", e);
        }
        finally {
            if (database != null) {
                database.close();
            }
        }

        return RemoteOpState.Failed;
    }
}
