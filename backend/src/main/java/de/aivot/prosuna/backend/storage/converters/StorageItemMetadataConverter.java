package de.aivot.prosuna.backend.storage.converters;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.storage.models.StorageItemMetadata;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Converter
@Component
public class StorageItemMetadataConverter implements AttributeConverter<StorageItemMetadata, String> {
    private final JsonMapper jsonMapper;

    public StorageItemMetadataConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String convertToDatabaseColumn(StorageItemMetadata baseElement) {
        try {
            return jsonMapper.writeValueAsString(baseElement);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StorageItemMetadata convertToEntityAttribute(String s) {
        try {
            return jsonMapper.readValue(s, StorageItemMetadata.class);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
}
