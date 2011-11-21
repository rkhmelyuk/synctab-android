package com.khmlabs.synctab.tag;

import com.khmlabs.synctab.SyncTabApplication;

import java.util.List;

/**
 * Manager to handle Tags.
 *
 * @author Ruslan Khmelyuk
 */
public class TagManager {

    private final SyncTabApplication application;
    private final RemoteTagManager remote;

    public TagManager(SyncTabApplication application, RemoteTagManager remote) {
        this.application = application;
        this.remote = remote;
    }

    /**
     * Gets the list of available tags.
     * @return the list of available tags.
     */
    public List<Tag> getTags() {
        // TODO - implement
        return null;
    }

    /**
     * Gets the tag by its id.
     *
     * @param id the tag id.
     * @return the found tag by id.
     */
    public Tag getTag(String id) {
        return null;
    }

}
