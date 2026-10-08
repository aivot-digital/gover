package de.aivot.prosuna.backend.process.dtos;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.ProcessNodePortResolution;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Node authoring data with computed, read-only outgoing ports. */
public record ProcessNodeResponseDTO(
        @Nonnull Integer id,
        @Nonnull Integer processId,
        @Nonnull Integer processVersion,
        @Nullable String name,
        @Nonnull String displayName,
        @Nullable String description,
        @Nonnull String dataKey,
        @Nonnull String processNodeDefinitionKey,
        @Nonnull Integer processNodeDefinitionVersion,
        @Nonnull AuthoredElementValues configuration,
        @Nonnull Map<String, String> outputMappings,
        @Nullable Integer timeLimitDays,
        @Nonnull Boolean timeLimitStrict,
        @Nullable String requirements,
        @Nullable String notes,
        @Nonnull Boolean savedWithErrors,
        @Nonnull Instant created,
        @Nonnull Instant updated,
        @Nonnull List<ProcessNodePort> ports,
        @Nonnull List<String> portResolutionProblems
) {
    @Nonnull
    public static ProcessNodeResponseDTO of(@Nonnull ProcessNodeEntity node, @Nonnull ProcessNodePortResolution resolution) {
        return new ProcessNodeResponseDTO(
                node.getId(),
                node.getProcessId(),
                node.getProcessVersion(),
                node.getName(),
                node.getDisplayName(),
                node.getDescription(),
                node.getDataKey(),
                node.getProcessNodeDefinitionKey(),
                node.getProcessNodeDefinitionVersion(),
                node.getConfiguration(),
                node.getOutputMappings(),
                node.getTimeLimitDays(),
                node.getTimeLimitStrict(),
                node.getRequirements(),
                node.getNotes(),
                node.getSavedWithErrors(),
                node.getCreated(),
                node.getUpdated(),
                resolution.ports(), resolution.problems()
        );
    }
}
