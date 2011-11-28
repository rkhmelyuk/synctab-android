package com.khmlabs.synctab.tag;

import com.khmlabs.synctab.util.StringUtil;

/**
 * @author Ruslan Khmelyuk
 */
public class Tag {

    private Integer id;
    private String tagId;
    private String name;

    public Tag() {
    }

    public Tag(Integer id, String tagId, String name) {
        this.id = id;
        this.tagId = tagId;
        this.name = name;
    }

    public Tag(String tagId, String name) {
        this(null, tagId, name);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLocal() {
        return StringUtil.isEmpty(tagId);
    }
}
