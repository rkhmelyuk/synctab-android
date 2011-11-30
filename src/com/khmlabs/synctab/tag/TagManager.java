package com.khmlabs.synctab.tag;

import android.util.Log;
import com.khmlabs.synctab.AppConstants;
import com.khmlabs.synctab.RemoteOpStatus;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.db.SyncTabDatabase;
import com.khmlabs.synctab.queue.QueueTask;
import com.khmlabs.synctab.queue.TaskType;

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
     *         <p/>
     *         TODO - think to cache
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

    /**
     * Add new tag with specified name.
     *
     * @param name the new tag name.
     * @return the remote operation status.
     */
    public RemoteOpStatus addTag(String name) {
        SyncTabDatabase database = null;
        try {
            database = new SyncTabDatabase(application);
            if (application.isOnLine()) {
                String id = remote.addTag(name);
                if (id != null) {
                    database.addTag(id, name);
                    return RemoteOpStatus.Success;
                }
            }

            final Tag tag = database.addTag(null, name);
            return application.getTaskQueueManager().addAddTagTask(tag.getId());
        }
        catch (Exception e) {
            Log.e(TAG, "Error to add a new tag.");
        }
        finally {
            if (database != null) {
                database.close();
            }
        }

        return RemoteOpStatus.Failed;
    }

    /**
     * Rename the specified tag.
     *
     * @param id      the id of the tag in local sqlite database.
     * @param newName the new name for tag.
     * @return the remote operation status.
     */
    public RemoteOpStatus renameTag(int id, String newName) {
        SyncTabDatabase database = null;
        try {
            database = new SyncTabDatabase(application);

            Tag tag = database.getTag(id);
            if (tag == null) {
                // nothing to rename... success?!
                return RemoteOpStatus.Success;
            }

            // update database
            tag.setName(newName);
            database.updateTag(tag);

            if (!tag.isLocal()) {
                if (application.isOnLine()) {
                    if (remote.renameTag(tag.getTagId(), newName)) {
                        return RemoteOpStatus.Success;
                    }
                }

                // enqueue only for existing tags, for new tags queue task has id dependency
                return application.getTaskQueueManager().addRenameTagTask(tag.getTagId(), newName);
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to rename a tag.");
        }
        finally {
            if (database != null) {
                database.close();
            }
        }

        return RemoteOpStatus.Failed;
    }

    /**
     * Remove the specified tag.
     *
     * @param id the id of the tag in local sqlite database.
     * @return the remote operation status.
     */
    public RemoteOpStatus removeTag(int id) {
        SyncTabDatabase database = null;
        try {
            database = new SyncTabDatabase(application);

            Tag tag = database.getTag(id);
            if (tag == null) {
                // nothing to remove... success?!
                return RemoteOpStatus.Success;
            }

            // remove from local database
            database.removeTag(id);
            if (AppConstants.LOG) {
                Log.i(TAG, "Removed tag locally by id " + id);
            }

            if (!tag.isLocal()) {

                // -- remove remotely for non-local tasks
                if (application.isOnLine()) {
                    if (remote.removeTag(tag.getTagId())) {
                        if (AppConstants.LOG) {
                            Log.i(TAG, "Removed tag remotely by id " + id);
                        }
                        return RemoteOpStatus.Success;
                    }
                }

                return application.getTaskQueueManager().addRemoveTabTask(tag.getTagId());
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to remove a tag.");
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
        if (task != null) {
            final TaskType type = task.getType();

            if (type == TaskType.AddTag) {
                return executeAddTagTask(task);
            }
            if (type == TaskType.RemoveTag) {
                return remote.removeTag(task.getParam1());
            }
            if (type == TaskType.RenameTag) {
                return remote.renameTag(task.getParam1(), task.getParam2());
            }
        }

        return false;
    }

    private boolean executeAddTagTask(QueueTask task) {
        SyncTabDatabase database = null;
        try {
            database = new SyncTabDatabase(application);

            Integer tagId = Integer.valueOf(task.getParam1());
            if (tagId == null) {
                // wrong id, no sense to enqueue it further
                return true;
            }
            Tag tag = database.getTag(tagId);
            if (tag == null) {
                // was removed already
                return true;
            }

            String id = remote.addTag(tag.getName());
            if (id != null) {
                tag.setTagId(id);
                database.updateTag(tag);

                return true;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to add a tag.");
        }
        finally {
            if (database != null) {
                database.close();
            }
        }

        return false;
    }

}
