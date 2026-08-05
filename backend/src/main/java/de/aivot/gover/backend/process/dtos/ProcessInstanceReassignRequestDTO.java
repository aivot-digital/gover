package de.aivot.gover.backend.process.dtos;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;

public record ProcessInstanceReassignRequestDTO(
        @Nullable
        @Size(min = 36, max = 36, message = "Die Benutzer-ID des Zuständigen muss 36 Zeichen lang sein.")
        String assignedUserId
) {
}
