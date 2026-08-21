package de.aivot.prosuna.backend.process.converters;


import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.models.lib.DiffItem;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedList;
import java.util.List;

@Converter
@Component
public class DiffItemsConverter implements AttributeConverter<List<DiffItem>, String> {

    private final JsonMapper jsonMapper;

    public DiffItemsConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Nonnull
    @Override
    public String convertToDatabaseColumn(@Nullable List<DiffItem> attributes) {
        if (attributes == null) {
            return "[]";
        }

        String dbData;
        try {
            dbData = jsonMapper.writeValueAsString(attributes);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }

        return dbData;
    }

    @Nonnull
    @Override
    public List<DiffItem> convertToEntityAttribute(@Nullable String dbData) {
        if (dbData == null) {
            return new LinkedList<>();
        }

        var objectMapper = jsonMapper
                .readerForListOf(DiffItem.class);

        List<DiffItem> mappings;
        try {
            mappings = objectMapper
                    .readValue(dbData);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }

        return mappings;
    }
}
