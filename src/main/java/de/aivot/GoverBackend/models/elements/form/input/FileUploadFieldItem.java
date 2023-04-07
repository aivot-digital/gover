package de.aivot.GoverBackend.models.elements.form.input;

import com.sun.istack.Nullable;

public class FileUploadFieldItem {
    private String name;
    private String uri;
    private Integer size;

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Nullable
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
