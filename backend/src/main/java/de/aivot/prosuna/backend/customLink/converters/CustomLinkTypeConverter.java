package de.aivot.prosuna.backend.customLink.converters;

import de.aivot.prosuna.backend.customLink.enums.CustomLinkType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/** Persists custom link types independently of their enum declaration order. */
@Converter
public class CustomLinkTypeConverter implements AttributeConverter<CustomLinkType, Short> {
    private static final Map<Short, CustomLinkType> TYPES_BY_DATABASE_VALUE = Arrays
            .stream(CustomLinkType.values())
            .collect(Collectors.toUnmodifiableMap(
                    CustomLinkType::getDatabaseValue,
                    type -> type
            ));

    @Override
    public Short convertToDatabaseColumn(CustomLinkType type) {
        return type == null ? null : type.getDatabaseValue();
    }

    @Override
    public CustomLinkType convertToEntityAttribute(Short databaseValue) {
        if (databaseValue == null) {
            return null;
        }

        var type = TYPES_BY_DATABASE_VALUE.get(databaseValue);
        if (type == null) {
            throw new IllegalArgumentException("Unknown custom link type database value: " + databaseValue);
        }
        return type;
    }
}
