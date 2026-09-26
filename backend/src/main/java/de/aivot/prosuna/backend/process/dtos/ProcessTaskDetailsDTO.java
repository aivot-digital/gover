package de.aivot.prosuna.backend.process.dtos;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Task readers receive display metadata, never the configuration of the underlying process model.
 */
public record ProcessTaskDetailsDTO(
        @Nonnull ProcessInstanceTaskEntity task,
        @Nonnull ProcessInstanceEntity instance,
        @Nonnull ProcessSummary process,
        @Nullable NodeSummary node,
        @Nullable ProviderSummary provider
) {
    public record ProcessSummary(@Nonnull Integer id, @Nonnull String internalTitle) {
    }

    public record NodeSummary(@Nullable String name, @Nullable String description) {
    }

    public record ProviderSummary(@Nonnull String key, @Nonnull String componentKey,
                                  @Nonnull String name, @Nonnull String abstractDescription) {
    }
}
