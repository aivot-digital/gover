package de.aivot.prosuna.backend.core.converters;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Converter
@Component
public class AuthoredElementValuesConverter implements AttributeConverter<AuthoredElementValues, String> {
    private final JsonMapper jsonMapper;

    public AuthoredElementValuesConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String convertToDatabaseColumn(AuthoredElementValues baseElement) {
        try {
            return jsonMapper.writeValueAsString(baseElement);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AuthoredElementValues convertToEntityAttribute(String s) {
        try {
            return jsonMapper.readValue(s, AuthoredElementValues.class);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
}
