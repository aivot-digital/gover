package de.aivot.GoverBackend.audit.models;

import jakarta.annotation.Nonnull;

public record AuditLogFilterActorOption(
        @Nonnull String value,
        @Nonnull String label
) {
}
