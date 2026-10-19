package de.aivot.prosuna.backend.core.converters;

import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Converter
@Component
public class EffectiveElementValuesConverter implements AttributeConverter<EffectiveElementValues, String> {
    private final JsonMapper jsonMapper;

    public EffectiveElementValuesConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String convertToDatabaseColumn(EffectiveElementValues baseElement) {
        try {
            return jsonMapper.writeValueAsString(baseElement);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public EffectiveElementValues convertToEntityAttribute(String s) {
        try {
            return jsonMapper.readValue(s, EffectiveElementValues.class);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
}
