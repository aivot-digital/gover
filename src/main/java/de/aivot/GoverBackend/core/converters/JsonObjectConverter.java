package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter
public class JsonObjectConverter implements AttributeConverter<Map<String, Object>, String> {
    /**
     * Database JSON has to preserve explicit null map entries so persisted drafts can keep
     * "cleared" authored values across reloads. The shared API mapper intentionally omits nulls,
     * so persistence uses a dedicated copy with null inclusion enabled.
     */
    private static final ObjectMapper DATABASE_JSON_MAPPER = ObjectMapperFactory
            .getNullPreservingInstance();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> baseElement) {
        try {
            return DATABASE_JSON_MAPPER.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String s) {
        try {
            return (Map<String, Object>) DATABASE_JSON_MAPPER.readValue(s, Map.class); // TODO: Check cast
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
