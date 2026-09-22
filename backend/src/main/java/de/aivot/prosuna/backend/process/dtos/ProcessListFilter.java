package de.aivot.prosuna.backend.process.dtos;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProcessListFilter(
        @Nullable @Size(max = 200) String search,
        @Nullable String view,
        @Nullable @Positive Integer processId,
        @Nullable @Positive Integer processVersion,
        @Nullable @Positive Long instanceId,
        @Nullable @Size(max = 36) String assignee
) {
}
