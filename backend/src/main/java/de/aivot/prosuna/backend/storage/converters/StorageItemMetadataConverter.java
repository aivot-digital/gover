package de.aivot.prosuna.backend.storage.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.storage.models.StorageItemMetadata;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StorageItemMetadataConverter implements AttributeConverter<StorageItemMetadata, String> {
    @Override
    public String convertToDatabaseColumn(StorageItemMetadata baseElement) {
        var mapper = JsonMapperFactory
                .getInstance();
        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StorageItemMetadata convertToEntityAttribute(String s) {
        var mapper = JsonMapperFactory
                .getInstance();

        try {
            return mapper.readValue(s, StorageItemMetadata.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
