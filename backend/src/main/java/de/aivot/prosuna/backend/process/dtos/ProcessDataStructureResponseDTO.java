package de.aivot.prosuna.backend.process.dtos;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public record ProcessDataStructureResponseDTO(
        @Nonnull List<Entry> entries,
        @Nonnull List<Issue> issues
) {
    public record Origin(@Nonnull Integer nodeId, @Nonnull String dataKey, @Nullable String name) {
    }

    public enum Category {ProcessData, ElementData, ProtectedProcessData}

    public record Entry(@Nonnull Category category, @Nonnull String path, @Nonnull String label, @Nullable String description,
                        @Nullable Origin origin, boolean unexpandedStructure) {
    }

    public enum IssueKind {MetadataUnavailable, InvalidPath}

    public record Issue(@Nonnull IssueKind kind, @Nonnull Origin origin, @Nullable String path, @Nonnull Category category) {
    }
}
