package com.khmlabs.synctab.tab;

import android.util.Log;

import com.khmlabs.synctab.FaviconPreloader;
import com.khmlabs.synctab.RemoteOpStatus;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.db.SyncTabDatabase;
import com.khmlabs.synctab.queue.QueueTask;
import com.khmlabs.synctab.queue.TaskType;

import java.util.Collections;
import java.util.List;

/**
 * @author Ruslan Khmelyuk
 */
public class TabManager {

    private static final String TAG = "TabManager";

    private final SyncTabApplication application;
    private final RemoteTabManager remote;

    public TabManager(SyncTabApplication application, RemoteTabManager remote) {
        this.remote = remote;
        this.application = application;
    }

    public RemoteOpStatus enqueueSync(String link, String tagId) {
        if (application.isOnLine() && remote.shareTab(link, tagId)) {
            return RemoteOpStatus.Success;
        }

        return application.getTaskQueueManager().addShareTabTask(link, tagId);
    }

    public boolean refreshSharedTabs() {
        try {
            List<SharedTab> tabs = remote.getSharedTabs();
            if (tabs != null) {
                handleRecentSharedTabs(tabs);
                return true;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Failed to refresh shared tabs.", e);
        }

        return false;
    }

    /**
     * Returns the list of shared tabs received by specified tag.
     *
     * If there is no  current tag, than nothing returns.
     *
     * @return the list of received tabs.
     */
    public List<SharedTab> receiveSharedTabs() {
        try {
            String tagId = application.getCurrentTag();
            if (tagId != null) {
                List<SharedTab> tabs = remote.receiveSharedTabs(tagId);
                if (tabs != null) {
                    return tabs;
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Failed to receive shared tabs.", e);
        }

        return Collections.emptyList();
    }

    public boolean loadOlderSharedTabs() {
        try {
            List<SharedTab> tabs = remote.getOlderSharedTabs();
            if (tabs != null) {
                if (tabs.size() > 0) {
                    insertSharedTabs(tabs);
                    updateOldestSharedTabId(tabs);
                    cacheFavicons(tabs);
                }
                else {
                    application.setOldestSharedTabId(null);
                }
                return true;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Failed to older shared tabs.", e);
        }

        return false;
    }

    private void handleRecentSharedTabs(List<SharedTab> sharedTabs) throws Exception {
        if (sharedTabs.size() > 0) {
            insertSharedTabs(sharedTabs);
            updateRecentSharedTabId(sharedTabs);
            updateOldestSharedTabId(sharedTabs);
            cacheFavicons(sharedTabs);
        }
    }

    private void insertSharedTabs(List<SharedTab> sharedTabs) {
        if (sharedTabs.size() > 0) {
            SyncTabDatabase database = null;
            try {
                database = new SyncTabDatabase(application);
                database.replaceSharedTabs(sharedTabs);
            }
            finally {
                if (database != null) {
                    database.close();
                }
            }
        }
    }

    private void updateOldestSharedTabId(List<SharedTab> sharedTabs) {
        final SharedTab oldest = SharedTabUtil.getOldestSharedTab(sharedTabs);
        if (oldest != null) {
            application.setOldestSharedTabId(oldest.getId());
        }
        else {
            application.setOldestSharedTabId(null);
        }
    }

    private void updateRecentSharedTabId(List<SharedTab> sharedTabs) {
        final SharedTab recent = SharedTabUtil.getRecentSharedTab(sharedTabs);
        if (recent != null) {
            application.setLastSharedTabId(recent.getId());
        }
        else {
            application.setLastSharedTabId(null);
        }
    }

    private void cacheFavicons(List<SharedTab> tabs) {
        new FaviconPreloader(application).preloadForTabs(tabs);
    }

    public RemoteOpStatus removeSharedTab(int tabId) {
        SyncTabDatabase database = null;
        try {
            database = new SyncTabDatabase(application);
            SharedTab sharedTab = database.getSharedTabById(tabId);
            if (sharedTab != null) {
                database.removeSharedTab(tabId);

                if (application.isOnLine()) {
                    if (remote.removeSharedTab(sharedTab.getId())) {
                        return RemoteOpStatus.Success;
                    }
                }

                return application.getTaskQueueManager().addRemoveTabTask(sharedTab.getId());
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to remove shared tab.");
            return RemoteOpStatus.Failed;
        }
        finally {
            if (database != null) {
                database.close();
            }
        }

        return RemoteOpStatus.Success;
    }

    public RemoteOpStatus reshareTab(int tabId) {
        SyncTabDatabase database = null;
        try {
            database = new SyncTabDatabase(application);
            SharedTab sharedTab = database.getSharedTabById(tabId);
            if (sharedTab != null) {
                sharedTab.setTimestamp(System.currentTimeMillis());
                database.replaceSharedTab(sharedTab);

                if (application.isOnLine()) {
                    if (remote.reshareTab(sharedTab.getId())) {
                        return RemoteOpStatus.Success;
                    }
                }

                return application.getTaskQueueManager().addReshareTabTask(sharedTab.getId());
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to reshare tab.");
            return RemoteOpStatus.Failed;
        }
        finally {
            if (database != null) {
                database.close();
            }
        }

        return RemoteOpStatus.Failed;
    }

    /**
     * Executes a queued sync task.
     *
     * @param task the task to execute.
     * @return true if was executed.
     */
    public boolean executeTask(QueueTask task) {
        final TaskType type = task.getType();

        if (type == TaskType.SyncTab) {
            return remote.shareTab(task.getParam1(), task.getParam2());
        }
        if (type == TaskType.RemoveSharedTab) {
            return remote.removeSharedTab(task.getParam1());
        }
        if (type == TaskType.ReshareTab) {
            return remote.reshareTab(task.getParam1());
        }
        if (type == TaskType.LoadFavicon) {
            FaviconPreloader loader = new FaviconPreloader(application);
            return loader.preloadFavicon(task.getParam1());
        }

        return false;
    }

}
