package de.aivot.GoverBackend.audit.models;

import jakarta.annotation.Nonnull;

import java.util.List;

public record AuditLogFilterOptions(
        @Nonnull List<String> modules,
        @Nonnull List<String> triggerTypes,
        @Nonnull List<AuditLogFilterActorOption> actors
) {
}
