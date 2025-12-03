package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;
import java.io.Serializable;

public class FileUploadFieldItem implements Serializable {
    @Nullable
    private String name;
    @Nullable
    private String uri;
    @Nullable
    private Integer size;

    @Nullable
    public String getName() {
        return name;
    }

    public FileUploadFieldItem setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getUri() {
        return uri;
    }

    public FileUploadFieldItem setUri(@Nullable String uri) {
        this.uri = uri;
        return this;
    }

    @Nullable
    public Integer getSize() {
        return size;
    }

    public FileUploadFieldItem setSize(@Nullable Integer size) {
        this.size = size;
        return this;
    }
}
