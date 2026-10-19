package de.aivot.prosuna.backend.process.converters;

import de.aivot.prosuna.backend.process.models.ProcessNodeDeadlineConfiguration;
import jakarta.annotation.Nonnull;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Converter
@Component
public class ProcessNodeDeadlinesConverter implements AttributeConverter<Map<String, ProcessNodeDeadlineConfiguration>, String> {
    private final JsonMapper jsonMapper;

    public ProcessNodeDeadlinesConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Nonnull
    public String convertToDatabaseColumn(@Nonnull Map<String, ProcessNodeDeadlineConfiguration> deadlines) {
        return jsonMapper.writeValueAsString(deadlines);
    }

    @Override
    @Nonnull
    public Map<String, ProcessNodeDeadlineConfiguration> convertToEntityAttribute(@Nonnull String value) {
        return jsonMapper.readValue(value, new TypeReference<>() {});
    }
}
