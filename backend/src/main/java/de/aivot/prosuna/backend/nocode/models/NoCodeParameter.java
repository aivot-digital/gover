package de.aivot.prosuna.backend.nocode.models;


import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;

public record NoCodeParameter(
        NoCodeDataType type,
        String label,
        String description,
        NoCodeParameterOption... options
) {
}
