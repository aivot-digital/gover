package de.aivot.prosuna.backend.process.projections;

import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Scalar query result, independent of task entities already cached in the persistence context.
 */
public record ProcessTaskAssignmentProjection(
        @Nonnull Long id,
        @Nonnull Integer processNodeId,
        @Nonnull ProcessTaskStatus status,
        @Nullable String assignedUserId
) {
}
