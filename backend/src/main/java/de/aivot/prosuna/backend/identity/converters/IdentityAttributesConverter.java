package de.aivot.prosuna.backend.identity.converters;

import de.aivot.prosuna.backend.identity.models.IdentityAttributeMapping;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * JPA attribute converter for handling the conversion of a list of {@link IdentityAttributeMapping} objects to a JSON string for database storage and vice versa.
 *
 * <p>This converter is used to seamlessly serialize and deserialize
 * {@link IdentityAttributeMapping} objects when persisting or retrieving data from a database column of type JSON.</p>
 *
 * <p>Key functionalities:</p>
 * <ul>
 *     <li>Converts a list of {@link IdentityAttributeMapping} objects into a JSON string
 *         for storage in the database.</li>
 *     <li>Converts a JSON string from the database back into a list of
 *         {@link IdentityAttributeMapping} objects.</li>
 *     <li>Uses Jackson's {@link com.fasterxml.jackson.databind.ObjectMapper} for
 *         JSON serialization and deserialization.</li>
 *     <li>Handles exceptions during the conversion process by throwing a
 *         {@link RuntimeException}.</li>
 * </ul>
 *
 * <p>This converter is annotated with {@link jakarta.persistence.Converter}
 * to indicate its use as a JPA attribute converter.</p>
 *
 * @see jakarta.persistence.AttributeConverter
 * @see IdentityAttributeMapping
 */
@Converter
@Component
public class IdentityAttributesConverter implements AttributeConverter<List<IdentityAttributeMapping>, String> {

    private final JsonMapper jsonMapper;

    public IdentityAttributesConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String convertToDatabaseColumn(List<IdentityAttributeMapping> attributes) {
        if (attributes == null) {
            return null;
        }


        String dbData;
        try {
            dbData = jsonMapper.writeValueAsString(attributes);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }

        return dbData;
    }

    @Override
    public List<IdentityAttributeMapping> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        var objectMapper = jsonMapper
                .readerForListOf(IdentityAttributeMapping.class);

        List<IdentityAttributeMapping> mappings;
        try {
            mappings = objectMapper
                    .readValue(dbData);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }

        return mappings;
    }
}
