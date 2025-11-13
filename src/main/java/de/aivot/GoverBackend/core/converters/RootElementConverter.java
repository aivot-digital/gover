package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.elements.RootElement;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RootElementConverter implements AttributeConverter<RootElement, String> {
    @Override
    public String convertToDatabaseColumn(RootElement baseElement) {
        var mapper = ObjectMapperFactory
                .getInstance();

        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RootElement convertToEntityAttribute(String s) {
        var mapper = ObjectMapperFactory
                .getInstance();

        try {
            return mapper.readValue(s, RootElement.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
