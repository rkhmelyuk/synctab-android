package com.khmlabs.synctab.queue;

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
