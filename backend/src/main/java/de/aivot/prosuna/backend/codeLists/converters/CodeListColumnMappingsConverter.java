package de.aivot.prosuna.backend.codeLists.converters;

import de.aivot.prosuna.backend.codeLists.models.CodeListColumnMapping;
import jakarta.annotation.Nonnull;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Converter
@Component
public class CodeListColumnMappingsConverter implements AttributeConverter<List<CodeListColumnMapping>, String> {
    private final JsonMapper jsonMapper;

    public CodeListColumnMappingsConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Nonnull
    public String convertToDatabaseColumn(@Nonnull List<CodeListColumnMapping> columnMappings) {
        return jsonMapper.writeValueAsString(columnMappings);
    }

    @Override
    @Nonnull
    public List<CodeListColumnMapping> convertToEntityAttribute(@Nonnull String value) {
        return jsonMapper.readValue(value, new TypeReference<>() {});
    }
}
