package com.khmlabs.synctab;

import org.apache.http.HttpHost;

import com.khmlabs.synctab.auth.AuthManager;
import com.khmlabs.synctab.auth.RemoteAuthManager;
import com.khmlabs.synctab.queue.QueueTask;
import com.khmlabs.synctab.tab.RemoteTabManager;
import com.khmlabs.synctab.tab.SharedTab;
import com.khmlabs.synctab.tab.TabManager;
import com.khmlabs.synctab.tag.RemoteTagManager;
import com.khmlabs.synctab.tag.Tag;
import com.khmlabs.synctab.tag.TagManager;

import java.util.List;

public class SyncTabFacade {

    private final TagManager tagManager;
    private final TabManager tabManager;
    private final AuthManager authManager;

    public SyncTabFacade(SyncTabApplication app, String scheme, String hostname, int port) {

        final HttpHost host = new HttpHost(hostname, port, scheme);

        tabManager = new TabManager(app, new RemoteTabManager(app, host));
        tagManager = new TagManager(app, new RemoteTagManager(app, host));
        authManager = new AuthManager(app, new RemoteAuthManager(app, host));
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

    public RemoteOpStatus enqueueSync(String link, String tagId) {
        return tabManager.enqueueSync(link, tagId);
    }

    public void refreshTags() {
        tagManager.refreshTags();
    }

    /**
     * Gets the list of shared tabs.
     * @return the list of shared tabs.
     */
    public List<SharedTab> receiveSharedTabs() {
        return tabManager.receiveSharedTabs();
    }

    /**
     * Gets the list of tags to show to user, when he/she shares a tab.
     * This is a place to filter not needed tags, sort them in correct order etc.
     *
     * @return the list of tags to show to user.
     */
    public List<Tag> getShareTags() {
        return tagManager.getShareTags();
    }

    /**
     * Gets the list of all available tags.
     *
     * @return the list of all tags.
     */
    public List<Tag> getTags() {
        return tagManager.getTags();
    }

    public boolean syncTask(QueueTask task) {
        if (task != null) {
            if (tabManager.executeTask(task)) {
                return true;
            }
            if (authManager.executeTask(task)) {
                return true;
            }
        }

        return false;
    }

    public Tag getTag(String tagId) {
        return tagManager.getTag(tagId);
    }
}
