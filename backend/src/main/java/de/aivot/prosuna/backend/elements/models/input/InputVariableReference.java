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

        // Keep invalid drafts representable; the resolver validates path syntax before any data access.
        path = path.trim();
        nodeDataKey = nodeDataKey == null ? null : nodeDataKey.trim();
    }
}
