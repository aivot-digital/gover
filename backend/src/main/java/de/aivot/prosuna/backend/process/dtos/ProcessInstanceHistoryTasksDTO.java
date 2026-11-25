package de.aivot.prosuna.backend.process.dtos;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import jakarta.annotation.Nonnull;
import org.springframework.data.domain.Page;

import java.util.List;

public record ProcessInstanceHistoryTasksDTO(
        @Nonnull Page<ProcessInstanceTaskEntity> tasks,
        @Nonnull List<ProcessInstanceTaskEntity> predecessors,
        @Nonnull List<ActiveTask> activeTasks
) {
    public record ActiveTask(@Nonnull Integer processNodeId, @Nonnull Integer processVersion,
                             @Nonnull ProcessTaskStatus status) {
    }
}
