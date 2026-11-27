package de.aivot.prosuna.backend.elements.models.input;

import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;

/**
 * Stores a source-relative path so display syntax can evolve independently from persisted references.
 *
 * <p>{@code markdown} marks a value from a Markdown input, as the variable picker knew it when the reference was
 * chosen. Fields that only show text then receive the value as plain text instead of its Markdown source.</p>
 */
public record InputVariableReference(
        @Nonnull InputVariableSource source,
        @Nonnull String path,
        @Nullable String nodeDataKey,
        @Nonnull Boolean markdown
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
        // References stored before the flag existed have no value for it.
        markdown = Boolean.TRUE.equals(markdown);
    }

    public InputVariableReference(@Nonnull InputVariableSource source, @Nonnull String path, @Nullable String nodeDataKey) {
        this(source, path, nodeDataKey, false);
    }
}
