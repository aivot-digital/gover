package de.aivot.prosuna.backend.process.converters;

import de.aivot.prosuna.backend.process.enums.ProcessWorkRequestKind;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Persists work request kinds with explicit values so changes to the enum declaration order do not alter stored data.
 */
@Converter
public class ProcessWorkRequestKindConverter implements AttributeConverter<ProcessWorkRequestKind, Short> {
    private static final Map<Short, ProcessWorkRequestKind> KINDS_BY_DATABASE_VALUE = Arrays
            .stream(ProcessWorkRequestKind.values())
            .collect(Collectors.toUnmodifiableMap(
                    ProcessWorkRequestKind::getDatabaseValue,
                    kind -> kind
            ));

    @Override
    public Short convertToDatabaseColumn(ProcessWorkRequestKind kind) {
        return kind == null ? null : kind.getDatabaseValue();
    }

    @Override
    public ProcessWorkRequestKind convertToEntityAttribute(Short databaseValue) {
        if (databaseValue == null) {
            return null;
        }

        var kind = KINDS_BY_DATABASE_VALUE.get(databaseValue);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown process work request kind database value: " + databaseValue);
        }
        return kind;
    }
}
