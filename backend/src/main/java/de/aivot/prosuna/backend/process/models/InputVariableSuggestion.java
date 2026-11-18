package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A variable offered by the variable picker. {@code markdown} marks values from Markdown inputs, which the picker
 * inserts as plain text unless they keep their formatting.
 */
public record InputVariableSuggestion(
        @Nonnull InputVariableSource source,
        @Nonnull String path,
        @Nullable String nodeDataKey,
        @Nonnull String label,
        @Nullable String description,
        @Nullable ProcessNodeEntity origin,
        boolean markdown
) {
    public InputVariableSuggestion(@Nonnull InputVariableSource source,
                                   @Nonnull String path,
                                   @Nullable String nodeDataKey,
                                   @Nonnull String label,
                                   @Nullable String description,
                                   @Nullable ProcessNodeEntity origin) {
        this(source, path, nodeDataKey, label, description, origin, false);
    }
}
