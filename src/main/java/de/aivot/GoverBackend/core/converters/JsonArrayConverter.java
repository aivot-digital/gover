package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;
import java.util.Map;

@Converter
public class JsonArrayConverter implements AttributeConverter<List<Map<String, Object>>, String> {
    @Override
    public String convertToDatabaseColumn(List<Map<String, Object>> baseElement) {
        var mapper = ObjectMapperFactory
                .getInstance();
        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Map<String, Object>> convertToEntityAttribute(String s) {
        var mapper = ObjectMapperFactory
                .getInstance();


        try {
            return (List<Map<String, Object>>) mapper
                    .readerForListOf(Map.class)
                    .readValue(s); // TODO: Check cast
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
