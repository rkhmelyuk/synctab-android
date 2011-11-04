package com.khmlabs.synctab.queue;

/**
 * The task in the queue. Usually, this is a task that need to be executed when device is online.
 *
 * Like share a tab A. But if device is off, new queue task is created.
 * When device is online, task is executed and removed from queue.
 *
 * Thus, the synchronization with remote server is implemented.
 */
public class QueueTask {

    private Integer id;
    private TaskType type;
    private String param;

    public QueueTask() {
    }

    public QueueTask(TaskType type, String param) {
        this.type = type;
        this.param = param;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }
}
