package de.aivot.GoverBackend.process.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.process.models.ProcessIdentityItem;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Converter
public class ProcessIdentityItemsConverter implements AttributeConverter<Map<String, ProcessIdentityItem>, String> {

    @Nonnull
    @Override
    public String convertToDatabaseColumn(@Nullable Map<String, ProcessIdentityItem> attributes) {
        if (attributes == null) {
            return "[]";
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
    public Map<String, ProcessIdentityItem> convertToEntityAttribute(@Nullable String dbData) {
        if (dbData == null) {
            return new HashMap<>();
        }

        var objectMapper = ObjectMapperFactory
                .getInstance();

        Map<String, ProcessIdentityItem> mappings;
        try {
            var map = objectMapper
                    .readValue(dbData, Map.class);

            mappings = new HashMap<>();
            for (var entry : map.entrySet()) {
                var key = ((Map.Entry<?, ?>) entry).getKey();
                var value = objectMapper.convertValue(((Map.Entry<?, ?>) entry).getValue(), ProcessIdentityItem.class);
                mappings.put(key.toString(), value);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return mappings;
    }
}
