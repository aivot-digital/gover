package de.aivot.prosuna.backend.process.dtos;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProcessListFilter(
        @Nullable @Size(max = 200, message = "Der Suchbegriff darf höchstens 200 Zeichen enthalten.") String search,
        @Nullable String view,
        @Nullable @Positive(message = "Die Prozess-ID muss größer als 0 sein.") Integer processId,
        @Nullable @Positive(message = "Die Prozessversion muss größer als 0 sein.") Integer processVersion,
        @Nullable @Positive(message = "Die Vorgangs-ID muss größer als 0 sein.") Long instanceId,
        @Nullable @Size(max = 36, message = "Die Konto-ID der zuständigen Person darf höchstens 36 Zeichen enthalten.") String assignee,
        @Nullable Boolean includeTests
) {
}
