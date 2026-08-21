package de.aivot.prosuna.backend.identity.converters;

import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Converter
@Component
public class IdentityDataMapConverter implements AttributeConverter<IdentityDataMap, String> {

    private final JsonMapper jsonMapper;

    public IdentityDataMapConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Nonnull
    @Override
    public String convertToDatabaseColumn(@Nullable IdentityDataMap attributes) {
        if (attributes == null) {
            return "{}";
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
    public IdentityDataMap convertToEntityAttribute(@Nullable String dbData) {
        if (dbData == null) {
            return new IdentityDataMap();
        }

        try {
            return jsonMapper
                    .readValue(dbData, IdentityDataMap.class);

        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
}
