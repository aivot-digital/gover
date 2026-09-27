package de.aivot.prosuna.backend.storage.converters;

import de.aivot.prosuna.backend.storage.models.StorageProviderMetadataAttribute;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Converter
@Component
public class StorageProviderMetadataAttributesConverter implements AttributeConverter<List<StorageProviderMetadataAttribute>, String> {
    private final JsonMapper jsonMapper;

    public StorageProviderMetadataAttributesConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String convertToDatabaseColumn(List<StorageProviderMetadataAttribute> baseElement) {
        try {
            return jsonMapper.writeValueAsString(baseElement);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<StorageProviderMetadataAttribute> convertToEntityAttribute(String s) {
        var mapper = jsonMapper
                .readerForListOf(StorageProviderMetadataAttribute.class);

        try {
            return mapper.readValue(s);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
}
