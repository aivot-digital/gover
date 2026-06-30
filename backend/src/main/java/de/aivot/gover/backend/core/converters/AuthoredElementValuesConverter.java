package de.aivot.gover.backend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AuthoredElementValuesConverter implements AttributeConverter<AuthoredElementValues, String> {
    @Override
    public String convertToDatabaseColumn(AuthoredElementValues baseElement) {
        var mapper = ObjectMapperFactory
                .getNullPreservingInstance();

        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AuthoredElementValues convertToEntityAttribute(String s) {
        var mapper = ObjectMapperFactory
                .getNullPreservingInstance();

        try {
            return mapper.readValue(s, AuthoredElementValues.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
