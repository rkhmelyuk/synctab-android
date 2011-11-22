package com.khmlabs.synctab.tag;

import android.util.Log;

import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.db.SyncTabDatabase;

import java.util.Collections;
import java.util.List;

/**
 * Manager to handle Tags.
 *
 * @author Ruslan Khmelyuk
 */
public class TagManager {

    private static final String TAG = "TagManager";

    private final SyncTabApplication application;
    private final RemoteTagManager remote;

    public TagManager(SyncTabApplication application, RemoteTagManager remote) {
        this.application = application;
        this.remote = remote;
    }

    /**
     * Gets the list of available tags.
     *
     * @return the list of available tags.
     */
    public List<Tag> getTags() {
        SyncTabDatabase database = null;
        try {
            database = new SyncTabDatabase(application);
            return database.getTags();
        }
        catch (Exception e) {
            Log.e(TAG, "Error to get the list of tags.", e);
        }
        finally {
            if (database != null) {
                database.close();
            }
        }

        return Collections.emptyList();
    }

    /**
     * Refresh the list of tags.
     *
     * @return true if was refreshed successfully.
     */
    public boolean refreshTags() {
        try {
            List<Tag> tags = remote.getTags();

            if (tags.size() > 0) {
                storeTags(tags);

                application.setTagsLoaded(true);
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to refresh tags", e);
        }

        return true;
    }

    /**
     * Stores tags in the database.
     * In fact it tries to replace existing tag if possible.
     *
     * @param tags the tags to store in database.
     */
    private void storeTags(List<Tag> tags) {
        SyncTabDatabase database = null;
        try {
            database = new SyncTabDatabase(application);
            database.replaceTags(tags);
        }
        finally {
            if (database != null) {
                database.close();
            }
        }
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
