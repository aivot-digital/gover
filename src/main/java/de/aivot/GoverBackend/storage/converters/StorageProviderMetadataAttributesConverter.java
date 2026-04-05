package de.aivot.GoverBackend.storage.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.storage.models.StorageProviderMetadataAttribute;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class StorageProviderMetadataAttributesConverter implements AttributeConverter<List<StorageProviderMetadataAttribute>, String> {
    @Override
    public String convertToDatabaseColumn(List<StorageProviderMetadataAttribute> baseElement) {
        var mapper = ObjectMapperFactory
                .getInstance();
        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<StorageProviderMetadataAttribute> convertToEntityAttribute(String s) {
        var mapper = ObjectMapperFactory
                .getInstance()
                .readerForListOf(StorageProviderMetadataAttribute.class);

        try {
            return mapper.readValue(s);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
