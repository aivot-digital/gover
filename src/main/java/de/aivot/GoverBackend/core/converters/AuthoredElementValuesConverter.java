package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter
public class AuthoredElementValuesConverter implements AttributeConverter<AuthoredElementValues, String> {
    @Override
    public String convertToDatabaseColumn(AuthoredElementValues baseElement) {
        var mapper = ObjectMapperFactory
                .getInstance();

        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AuthoredElementValues convertToEntityAttribute(String s) {
        var mapper =  ObjectMapperFactory
                .getInstance();

        try {
            return mapper.readValue(s, AuthoredElementValues.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
