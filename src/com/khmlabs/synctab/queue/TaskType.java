package com.khmlabs.synctab.queue;

/**
 * The type of the queue task.
 */
public enum TaskType {

    /** Logout on server, to cleanup server session data. */
    Logout(1),

    /** Share a tab. */
    SyncTab(2),

    /** Remote the shared tab. */
    RemoveSharedTab(3),

    /** Reshare a tab. */
    ReshareTab(4),

    /** Load a tab icon. */
    LoadFavicon(5),

    /** Add new tag. */
    AddTag(6),

    /** Rename the tag. */
    RenameTag(7),

    /** Remove the tag. */
    RemoveTag(8);

    private final int id;

    TaskType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static TaskType findById(int id) {
        for (TaskType each : values()) {
            if (each.id == id) {
                return each;
            }
        }
        return null;
    }
}
