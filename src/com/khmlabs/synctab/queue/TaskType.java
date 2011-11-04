package com.khmlabs.synctab.queue;

public enum TaskType {

    Logout(1),
    SyncTab(2),
    RemoveSharedTab(3),
    ReshareTab(4),
    LoadFavicon(5);

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
