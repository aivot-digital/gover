package de.aivot.prosuna.backend.nocode.models;


import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;

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
