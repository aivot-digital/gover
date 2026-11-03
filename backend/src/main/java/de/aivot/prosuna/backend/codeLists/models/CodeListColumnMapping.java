package de.aivot.prosuna.backend.codeLists.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Maps a stable column key to a display name and to the 1-based position of the column in the stored item cells.
 * <p>
 * Everything that references a code list column uses the key, so a source that reorders its columns does not change
 * the meaning of a reference. A missing index means that the column no longer exists in the source.
 */
public record CodeListColumnMapping(
        @Nonnull String key,
        @Nonnull String name,
        @Nullable Integer index
) {
    @Nonnull
    public CodeListColumnMapping withIndex(@Nullable Integer index) {
        return new CodeListColumnMapping(key, name, index);
    }
}
