package de.aivot.gover.backend.nocode.models;


import de.aivot.gover.backend.nocode.enums.NoCodeDataType;

public record NoCodeParameter(
        NoCodeDataType type,
        String label,
        String description,
        NoCodeParameterOption... options
) {
}
