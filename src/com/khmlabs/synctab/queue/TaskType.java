package com.khmlabs.synctab.queue;

public enum TaskType {

    Logout(1),
    SyncTab(2);

    private final int id;

    TaskType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
