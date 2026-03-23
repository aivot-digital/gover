package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class FileUploadInputElementItem implements Serializable {
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

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FileUploadInputElementItem that = (FileUploadInputElementItem) o;
        return Objects.equals(name, that.name) && Objects.equals(uri, that.uri) && Objects.equals(size, that.size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, uri, size);
    }

    // endregion

    // region Getters & Setters

    public FileUploadInputElementItem setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getUri() {
        return uri;
    }

    public FileUploadInputElementItem setUri(@Nullable String uri) {
        this.uri = uri;
        return this;
    }

    @Nullable
    public Integer getSize() {
        return size;
    }

    public FileUploadInputElementItem setSize(@Nullable Integer size) {
        this.size = size;
        return this;
    }

    // endregion
}
