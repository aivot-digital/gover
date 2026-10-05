package de.aivot.prosuna.backend.process.dtos;

import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.Map;

public record ProcessInstanceEventLogDTO(
        @Nonnull InstanceContext instance,
        @Nullable TaskContext task,
        @Nonnull Page<Entry> events
) {
    public record InstanceContext(
            long id,
            @Nonnull String caseNumber,
            @Nonnull Instant started,
            @Nullable Instant finished,
            @Nullable Long runtime
    ) {
    }

    public record TaskContext(
            long id,
            @Nonnull String name,
            @Nonnull Instant started,
            @Nullable Instant finished,
            @Nullable Long runtime
    ) {
    }

    public record Entry(
            long id,
            long processInstanceId,
            @Nullable Long processInstanceTaskId,
            @Nonnull ProcessNodeExecutionLogLevel level,
            boolean technical,
            boolean audit,
            @Nonnull String title,
            @Nonnull String message,
            @Nonnull Map<String, Object> details,
            @Nonnull Instant timestamp,
            @Nullable String triggeringUserId,
            @Nullable String triggeringUserName,
            @Nullable String processNodeName
    ) {
    }
}
