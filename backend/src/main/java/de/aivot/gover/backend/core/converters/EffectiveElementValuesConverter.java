package de.aivot.gover.backend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.elements.models.EffectiveElementValues;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EffectiveElementValuesConverter implements AttributeConverter<EffectiveElementValues, String> {
    @Override
    public String convertToDatabaseColumn(EffectiveElementValues baseElement) {
        var mapper = ObjectMapperFactory
                .getInstance();

        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public EffectiveElementValues convertToEntityAttribute(String s) {
        var mapper = ObjectMapperFactory
                .getInstance();

        try {
            return mapper.readValue(s, EffectiveElementValues.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
