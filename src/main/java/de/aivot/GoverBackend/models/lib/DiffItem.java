package de.aivot.GoverBackend.models.lib;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

public record DiffItem(
        @Nonnull String field,
        @Nullable Object oldValue,
        @Nullable Object newValue
) implements Serializable {
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DiffItem diffItem = (DiffItem) o;
        return Objects.equals(field, diffItem.field) && Objects.equals(oldValue, diffItem.oldValue) && Objects.equals(newValue, diffItem.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, oldValue, newValue);
    }
}
