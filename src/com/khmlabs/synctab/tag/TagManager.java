package com.khmlabs.synctab.tag;

import android.util.Log;

import com.khmlabs.synctab.AppConstants;
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

    public List<Tag> getShareTags() {
        final List<Tag> tags = getTags();

        if (tags.size() > 0) {
            // Remove current application tag
            // from the list of available to send to

            String tagId = application.getCurrentTag();
            if (tagId != null) {
                for (Tag each : tags) {
                    if (tagId.equals(each.getTagId())) {
                        tags.remove(each);
                        break;
                    }
                }
            }
        }

        return tags;
    }

    /**
     * Gets the list of available tags.
     *
     * @return the list of available tags.
     *
     * TODO - think to cache
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

                // tags loaded flag is set
                application.setTagsLoaded(true);

                // Find and set the current tag
                Tag currentTag = findCurrentTag(tags);
                if (currentTag != null) {
                    application.setCurrentTag(currentTag.getTagId());
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to refresh tags", e);
        }

        return true;
    }

    private Tag findCurrentTag(List<Tag> tags) {
        String tagId = application.getCurrentTag();
        if (tagId != null) {
            for (Tag each : tags) {
                if (tagId.equals(each.getTagId())) {
                    return each;
                }
            }
        }

        for (Tag each : tags) {
            if (AppConstants.ANDROID_TAG_NAME.equals(each.getName())) {
                return each;
            }
        }

        return null;
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
     *
     * TODO - remove if not used
     */
    public Tag getTag(String id) {
        SyncTabDatabase database = null;
        try {
            database = new SyncTabDatabase(application);
            return database.getTagById(id);
        }
        catch (Exception e) {
            Log.e(TAG, "Error to remote a tag by id", e);
            return null;
        }
        finally {
            if (database != null) {
                database.close();
            }
        }
    }

}
