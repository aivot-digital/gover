package de.aivot.prosuna.backend.system.dtos;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;

public record DashboardOverviewDTO(
        @Nonnull TaskSummary tasks,
        @Nonnull List<RecentProcess> recentProcesses
) {
    public record TaskSummary(
            long total,
            long overdue,
            @Nonnull List<Task> items
    ) {
    }

    public record Task(
            long id,
            long processInstanceId,
            int processId,
            int processVersion,
            @Nonnull String taskName,
            @Nonnull String processTitle,
            @Nonnull String caseNumber,
            @Nonnull Instant started,
            @Nullable Instant deadline
    ) {
    }

    public record RecentProcess(
            int id,
            @Nonnull String title,
            @Nullable Integer draftedVersion,
            @Nullable Integer publishedVersion,
            @Nonnull Instant updated
    ) {
    }
}
