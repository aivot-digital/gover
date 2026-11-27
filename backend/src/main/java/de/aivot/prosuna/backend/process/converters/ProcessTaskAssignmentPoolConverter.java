package de.aivot.prosuna.backend.process.converters;

import de.aivot.prosuna.backend.process.models.ProcessTaskAssignmentPool;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Converter
@Component
public class ProcessTaskAssignmentPoolConverter implements AttributeConverter<ProcessTaskAssignmentPool, String> {
    private final JsonMapper jsonMapper;

    public ProcessTaskAssignmentPoolConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Nullable
    public String convertToDatabaseColumn(@Nullable ProcessTaskAssignmentPool pool) {
        return pool != null ? jsonMapper.writeValueAsString(pool) : null;
    }

    @Override
    @Nullable
    public ProcessTaskAssignmentPool convertToEntityAttribute(@Nullable String value) {
        return value != null ? jsonMapper.readValue(value, ProcessTaskAssignmentPool.class) : null;
    }
}
