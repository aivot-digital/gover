package de.aivot.prosuna.backend.identity.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;

@Converter
public class IdentityDataMapConverter implements AttributeConverter<IdentityDataMap, String> {

    @Nonnull
    @Override
    public String convertToDatabaseColumn(@Nullable IdentityDataMap attributes) {
        if (attributes == null) {
            return "{}";
        }

        var objectMapper = ObjectMapperFactory
                .getInstance();

        String dbData;
        try {
            dbData = objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
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
            return ObjectMapperFactory
                    .getInstance()
                    .readValue(dbData, IdentityDataMap.class);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
