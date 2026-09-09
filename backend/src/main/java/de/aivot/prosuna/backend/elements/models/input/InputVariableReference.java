package de.aivot.prosuna.backend.elements.models.input;

import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;

/** Stores a source-relative path so display syntax can evolve independently from persisted references. */
public record InputVariableReference(
        @Nonnull InputVariableSource source,
        @Nonnull String path,
        @Nullable String nodeDataKey
) implements Serializable {
    public InputVariableReference {
        if (source == null) {
            throw new IllegalArgumentException("A variable reference requires a source.");
        }
        if (path == null) {
            throw new IllegalArgumentException("A variable reference requires a path.");
        }

        path = normalizePath(path);
        nodeDataKey = nodeDataKey == null ? null : nodeDataKey.trim();
    }

    private static String normalizePath(String path) {
        return String.join(".", java.util.Arrays.stream(path.trim().split("\\.", -1))
                .map(String::trim)
                .toList());
    }
}
