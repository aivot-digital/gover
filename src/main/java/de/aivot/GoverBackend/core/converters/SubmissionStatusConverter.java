package de.aivot.GoverBackend.core.converters;

import de.aivot.GoverBackend.enums.SubmissionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class SubmissionStatusConverter implements AttributeConverter<SubmissionStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(SubmissionStatus status) {
        if (status == null) {
            return null;
        }
        return status.getKey();
    }

    @Override
    public SubmissionStatus convertToEntityAttribute(Integer status) {
        if (status == null) {
            return null;
        }

        return Stream.of(SubmissionStatus.values())
                .filter(c -> c.matches(status))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
