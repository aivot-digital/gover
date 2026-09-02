package de.aivot.prosuna.backend.process.converters;

import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Persists task statuses with explicit values so changes to the enum declaration order do not alter stored data.
 */
@Converter
public class ProcessTaskStatusConverter implements AttributeConverter<ProcessTaskStatus, Short> {
    private static final Map<Short, ProcessTaskStatus> STATUSES_BY_DATABASE_VALUE = Arrays
            .stream(ProcessTaskStatus.values())
            .collect(Collectors.toUnmodifiableMap(
                    ProcessTaskStatus::getDatabaseValue,
                    status -> status
            ));

    @Override
    public Short convertToDatabaseColumn(ProcessTaskStatus status) {
        return status == null ? null : status.getDatabaseValue();
    }

    @Override
    public ProcessTaskStatus convertToEntityAttribute(Short databaseValue) {
        if (databaseValue == null) {
            return null;
        }

        var status = STATUSES_BY_DATABASE_VALUE.get(databaseValue);
        if (status == null) {
            throw new IllegalArgumentException("Unknown process task status database value: " + databaseValue);
        }
        return status;
    }
}
