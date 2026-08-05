package de.aivot.gover.backend.process.converters;

import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

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
