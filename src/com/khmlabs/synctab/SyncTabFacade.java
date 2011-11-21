package com.khmlabs.synctab;

import org.apache.http.HttpHost;

import com.khmlabs.synctab.auth.AuthManager;
import com.khmlabs.synctab.auth.RemoteAuthManager;
import com.khmlabs.synctab.queue.QueueTask;
import com.khmlabs.synctab.queue.TaskType;
import com.khmlabs.synctab.tab.RemoteTabManager;
import com.khmlabs.synctab.tab.TabManager;
import com.khmlabs.synctab.tag.RemoteTagManager;
import com.khmlabs.synctab.tag.TagManager;

public class SyncTabFacade {

    private final TagManager tagManager;
    private final TabManager tabManager;
    private final AuthManager authManager;

    private final RemoteTagManager remoteTagManager;
    private final RemoteTabManager remoteTabManager;
    private final RemoteAuthManager remoteAuthManager;

    private final SyncTabApplication application;

    public SyncTabFacade(SyncTabApplication app, String scheme, String hostname, int port) {
        this.application = app;

        final HttpHost host = new HttpHost(hostname, port, scheme);

        remoteTabManager = new RemoteTabManager(app, host);
        tabManager = new TabManager(app, remoteTabManager);

        remoteTagManager = new RemoteTagManager(app, host);
        tagManager = new TagManager(app, remoteTagManager);

        remoteAuthManager = new RemoteAuthManager(app, host);
        authManager = new AuthManager(app, remoteAuthManager);
    }

    public RegistrationStatus register(String email, String password) {
        return authManager.register(email, password);
    }

    public boolean authenticate(String email, String password) {
        return authManager.authenticate(email, password);
    }

    public void logout(String token) {
        authManager.logout(token);
    }

    public boolean refreshSharedTabs() {
        return tabManager.refreshSharedTabs();
    }

    public RemoteOpStatus removeSharedTab(int tabId) {
        return tabManager.removeSharedTab(tabId);
    }

    public RemoteOpStatus reshareTab(int tabId) {
        return tabManager.reshareTab(tabId);
    }

    public boolean loadOlderSharedTabs() {
        return tabManager.loadOlderSharedTabs();
    }

    public RemoteOpStatus enqueueSync(String link) {
        return tabManager.enqueueSync(link);
    }

    public boolean syncTask(QueueTask task) {
        if (task != null) {
            if (task.getType() == TaskType.SyncTab) {
                return remoteTabManager.shareTab(task.getParam());
            }
            else if (task.getType() == TaskType.Logout) {
                return remoteAuthManager.logout(task.getParam());
            }
            else if (task.getType() == TaskType.RemoveSharedTab) {
                return remoteTabManager.removeSharedTab(task.getParam());
            }
            else if (task.getType() == TaskType.ReshareTab) {
                return remoteTabManager.reshareTab(task.getParam());
            }
            else if (task.getType() == TaskType.LoadFavicon) {
                FaviconPreloader loader = new FaviconPreloader(application);
                return loader.preloadFavicon(task.getParam());
            }
        }

        return false;
    }
}
