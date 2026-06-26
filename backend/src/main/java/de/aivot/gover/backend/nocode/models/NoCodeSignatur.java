package de.aivot.gover.backend.nocode.models;


import de.aivot.gover.backend.nocode.enums.NoCodeDataType;

public record NoCodeSignatur(
        NoCodeDataType returnType,
        NoCodeParameter... parameters
) {
    public static NoCodeSignatur of(
            NoCodeDataType returnType,
            NoCodeParameter... parameters
    ) {
        return new NoCodeSignatur(returnType, parameters);
    }

    public static NoCodeSignatur[] of(
            NoCodeSignatur ... signatures
    ) {
        return signatures;
    }
}
