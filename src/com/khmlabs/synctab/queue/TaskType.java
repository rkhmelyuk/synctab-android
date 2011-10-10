package com.khmlabs.synctab.queue;

public enum TaskType {

    Logout(1),
    SyncTab(2),
    RemoveSharedTab(3);

    private final int id;

    TaskType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static TaskType findById(int id) {
        if (id == Logout.id) {
            return Logout;
        }
        else if (id == SyncTab.id) {
            return SyncTab;
        }
        return null;
    }
}
