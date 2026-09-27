package de.aivot.prosuna.backend.process.dtos;

import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;

public final class ProcessListDTO {
    private ProcessListDTO() {
    }

    public record Instance(
            @Nonnull Long id, @Nonnull String caseNumber, @Nonnull List<String> assignedFileNumbers,
            @Nonnull Integer processId, @Nonnull Integer processVersion, @Nonnull String processName,
            @Nullable String assignedUserId, @Nullable String assignedUserName,
            @Nonnull ProcessInstanceStatus status, @Nullable String statusOverride,
            @Nonnull Instant started, @Nullable Instant finished, boolean test
    ) {
    }

    public record Task(
            @Nonnull Long id, @Nonnull Long processInstanceId, @Nonnull String caseNumber,
            @Nonnull List<String> assignedFileNumbers, @Nonnull Integer processId,
            @Nonnull Integer processVersion, @Nonnull String processName,
            @Nonnull String taskName, @Nullable String taskType, @Nullable String description,
            @Nullable String assignedUserId, @Nullable String assignedUserName,
            @Nonnull ProcessTaskStatus status, @Nullable String statusOverride,
            @Nonnull Instant started, @Nullable Instant deadline, @Nullable Instant finished, boolean test
    ) {
    }

    public record Option(@Nonnull String value, @Nonnull String label) {
    }

    public record Options(@Nonnull List<Option> processes, @Nonnull List<Option> assignees) {
    }
}
