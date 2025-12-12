package de.aivot.GoverBackend.process.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.models.lib.DiffItem;
import de.aivot.GoverBackend.process.models.DeliveryChannelConfig;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@Converter
public class DeliveryChannelConfigsConverter implements AttributeConverter<List<DeliveryChannelConfig>, String> {

    @Nonnull
    @Override
    public String convertToDatabaseColumn(@Nullable List<DeliveryChannelConfig> attributes) {
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
    public List<DeliveryChannelConfig> convertToEntityAttribute(@Nullable String dbData) {
        if (dbData == null) {
            return new LinkedList<>();
        }

        var objectMapper = ObjectMapperFactory
                .getInstance()
                .readerForListOf(DeliveryChannelConfig.class);

        List<DeliveryChannelConfig> mappings;
        try {
            mappings = objectMapper
                    .readValue(dbData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return mappings;
    }
}
