package de.aivot.GoverBackend.nocode.models;

import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;

public record NoCodeParameter(NoCodeDataType type, String label, NoCodeParameterOption... options) {
}
