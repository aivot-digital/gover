package de.aivot.prosuna.backend.identity.converters;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.identity.models.IdentityProviderLink;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;

import java.util.LinkedList;
import java.util.List;

/**
 * JPA attribute converter for handling the conversion of a list of
 * {@link IdentityProviderLink} objects to a JSON string for database storage
 * and vice versa.
 *
 * <p>This converter is used to seamlessly serialize and deserialize
 * {@link IdentityProviderLink} objects when persisting or retrieving
 * data from a database column of type JSON.</p>
 *
 * <p>Key functionalities:</p>
 * <ul>
 *     <li>Converts a list of {@link IdentityProviderLink} objects into a JSON string
 *         for storage in the database.</li>
 *     <li>Converts a JSON string from the database back into a list of
 *         {@link IdentityProviderLink} objects.</li>
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
 * @see IdentityProviderLink
 */
@Converter
public class IdentityProviderLinksConverter implements AttributeConverter<List<IdentityProviderLink>, String> {

    @Override
    public String convertToDatabaseColumn(List<IdentityProviderLink> attributes) {
        if (attributes == null) {
            return "[]";
        }

        var objectMapper = JsonMapperFactory.getInstance();

        String dbData;
        try {
            dbData = objectMapper.writeValueAsString(attributes);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }

        return dbData;
    }

    @Override
    public List<IdentityProviderLink> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return new LinkedList<>();
        }

        var objectMapper = JsonMapperFactory.getInstance()
                .readerForListOf(IdentityProviderLink.class);

        List<IdentityProviderLink> mappings;
        try {
            mappings = objectMapper
                    .readValue(dbData);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }

        return mappings;
    }
}
