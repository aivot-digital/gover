package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class DomainAndUserSelectInputElementValue implements Serializable {
    @Nullable
    private String type;

    @Nullable
    private String id;

    public DomainAndUserSelectInputElementValue() {
    }

    public DomainAndUserSelectInputElementValue(@Nullable String type, @Nullable String id) {
        this.type = type;
        this.id = id;
    }

    public boolean isEmpty() {
        return type == null || type.isBlank() || id == null || id.isBlank();
    }

    @Nonnull
    public String toComparableKey() {
        return (type == null ? "" : type) + ":" + (id == null ? "" : id);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DomainAndUserSelectInputElementValue that = (DomainAndUserSelectInputElementValue) o;
        return Objects.equals(type, that.type) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id);
    }

    @Nullable
    public String getType() {
        return type;
    }

    public DomainAndUserSelectInputElementValue setType(@Nullable String type) {
        this.type = type;
        return this;
    }

    @Nullable
    public String getId() {
        return id;
    }

    public DomainAndUserSelectInputElementValue setId(@Nullable String id) {
        this.id = id;
        return this;
    }
}
