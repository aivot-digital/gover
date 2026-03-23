package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter
public class JsonObjectConverter implements AttributeConverter<Map<String, Object>, String> {
    @Override
    public String convertToDatabaseColumn(Map<String, Object> baseElement) {
        var mapper = ObjectMapperFactory
                .getInstance();
        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String s) {
        var mapper = ObjectMapperFactory
                .getInstance();

        try {
            return (Map<String, Object>) mapper.readValue(s, Map.class); // TODO: Check cast
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
