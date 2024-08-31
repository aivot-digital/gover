package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;

public class FileUploadFieldItem {
    private String name;
    private String uri;
    private Integer size;

    public FileUploadFieldItem(Map<String, Object> data) {
        name = MapUtils.getString(data, "name");
        uri = MapUtils.getString(data, "uri");
        size = MapUtils.getInteger(data, "size");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
