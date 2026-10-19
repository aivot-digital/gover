package de.aivot.prosuna.backend.payment.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.prosuna.backend.models.payment.PaymentProduct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedList;
import java.util.List;

/**
 * JPA attribute converter for handling the conversion of a list of {@link PaymentProduct} objects to a JSON string for database storage and vice versa.
 *
 * <p>This converter is used to seamlessly serialize and deserialize
 * {@link PaymentProduct} objects when persisting or retrieving data from a database column of type JSON.</p>
 *
 * <p>Key functionalities:</p>
 * <ul>
 *     <li>Converts a list of {@link PaymentProduct} objects into a JSON string
 *         for storage in the database.</li>
 *     <li>Converts a JSON string from the database back into a list of
 *         {@link PaymentProduct} objects.</li>
 *     <li>Uses Jackson's {@link ObjectMapper} for
 *         JSON serialization and deserialization.</li>
 *     <li>Handles exceptions during the conversion process by throwing a
 *         {@link RuntimeException}.</li>
 * </ul>
 *
 * <p>This converter is annotated with {@link Converter}
 * to indicate its use as a JPA attribute converter.</p>
 *
 * @see AttributeConverter
 * @see PaymentProduct
 */
@Converter
@Component
public class PaymentProductsConverter implements AttributeConverter<List<PaymentProduct>, String> {

    private final JsonMapper jsonMapper;

    public PaymentProductsConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String convertToDatabaseColumn(List<PaymentProduct> attributes) {
        if (attributes == null) {
            return "[]";
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
    public List<PaymentProduct> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return new LinkedList<>();
        }

        var objectMapper = jsonMapper
                .readerForListOf(PaymentProduct.class);

        List<PaymentProduct> mappings;
        try {
            mappings = objectMapper
                    .readValue(dbData);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }

        return mappings;
    }
}
