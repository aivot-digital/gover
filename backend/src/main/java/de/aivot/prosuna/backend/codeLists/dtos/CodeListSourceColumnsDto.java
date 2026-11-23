package de.aivot.prosuna.backend.codeLists.dtos;

import de.aivot.prosuna.backend.codeLists.models.CodeListColumnMapping;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/** The columns of a source and the mappings a new code list would receive for them. */
public record CodeListSourceColumnsDto(
        @Nonnull List<String> columns,
        @Nonnull List<CodeListColumnMapping> columnMappings,
        @Nullable String keyColumnKey
) {
}
