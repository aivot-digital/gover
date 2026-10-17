package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record InputVariableSuggestion(
        @Nonnull InputVariableSource source,
        @Nonnull String path,
        @Nullable String nodeDataKey,
        @Nonnull String label,
        @Nullable String description,
        @Nullable ProcessNodeEntity origin
) {
}
