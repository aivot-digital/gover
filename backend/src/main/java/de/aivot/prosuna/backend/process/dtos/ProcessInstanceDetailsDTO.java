package de.aivot.prosuna.backend.process.dtos;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.time.Instant;

public record ProcessInstanceDetailsDTO(
        @Nonnull ProcessInstanceEntity instance,
        @Nonnull String processName,
        @Nonnull Integer departmentId,
        @Nullable String departmentName,
        @Nonnull String triggerName,
        @Nullable String triggerType,
        @Nonnull List<ActiveTask> activeTasks
) {
    public record ActiveTask(
            @Nonnull Long id,
            @Nonnull String name,
            @Nonnull ProcessTaskStatus status,
            @Nullable String statusOverride,
            @Nullable String assignedUserId,
            @Nullable Instant deadline
    ) {
    }
}
