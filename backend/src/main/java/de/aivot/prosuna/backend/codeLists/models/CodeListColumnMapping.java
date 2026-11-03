package de.aivot.prosuna.backend.codeLists.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Maps a stable column key to a display name and to the 1-based position of the column in the stored item cells.
 * <p>
 * Everything that references a code list column uses the key, so a source that reorders its columns does not change
 * the meaning of a reference. A missing index means that the column no longer exists in the source. Since the key is
 * editable, such a mapping remembers its former source column in {@code formerSourceColumn}, so it can be bound to
 * that column again when the column returns.
 */
public record CodeListColumnMapping(
        @Nonnull String key,
        @Nonnull String name,
        @Nullable Integer index,
        @Nullable String formerSourceColumn
) {
    public CodeListColumnMapping(@Nonnull String key, @Nonnull String name, @Nullable Integer index) {
        this(key, name, index, null);
    }

    /** Binds the mapping to the source column at {@code index}. */
    @Nonnull
    public CodeListColumnMapping bindTo(int index) {
        return new CodeListColumnMapping(key, name, index, null);
    }

    /** Removes the index and remembers the source column the mapping pointed to. */
    @Nonnull
    public CodeListColumnMapping unbind(@Nullable String formerSourceColumn) {
        return new CodeListColumnMapping(key, name, null, formerSourceColumn);
    }
}
