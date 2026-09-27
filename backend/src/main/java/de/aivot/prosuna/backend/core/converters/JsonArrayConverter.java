package de.aivot.prosuna.backend.core.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@Converter
@Component
public class JsonArrayConverter implements AttributeConverter<List<Map<String, Object>>, String> {
    private final JsonMapper jsonMapper;

    public JsonArrayConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String convertToDatabaseColumn(List<Map<String, Object>> baseElement) {
        try {
            return jsonMapper.writeValueAsString(baseElement);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Map<String, Object>> convertToEntityAttribute(String s) {
        try {
            return jsonMapper
                    .readerForListOf(Map.class)
                    .readValue(s); // TODO: Check cast
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
}
