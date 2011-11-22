package com.khmlabs.synctab.tab;

/**
 * The shared tab, also called a shared link.
 *
 * @author Ruslan Khmelyuk
 */
public class SharedTab {

    /**
     * The local id of the shared tab.
     * Is also an id of the shared tabs table row.
     */
    private int rowId;

    /**
     * The remote id of the shared tab.
     * Used to remove, re-sync the remote entry too.
     */
    private String id;
    private String title;
    private String link;
    private String favicon;
    private String device;
    private String tagId;

    private long timestamp;

    public int getRowId() {
        return rowId;
    }

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getFavicon() {
        return favicon;
    }

    public void setFavicon(String favicon) {
        this.favicon = favicon;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }
}
