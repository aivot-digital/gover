package de.aivot.prosuna.backend.core.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Converter
@Component
public class JsonObjectConverter implements AttributeConverter<Map<String, Object>, String> {
    private final JsonMapper jsonMapper;

    public JsonObjectConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String convertToDatabaseColumn(Map<String, Object> baseElement) {
        try {
            return jsonMapper.writeValueAsString(baseElement);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String s) {
        try {
            return (Map<String, Object>) jsonMapper.readValue(s, Map.class); // TODO: Check cast
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
}
