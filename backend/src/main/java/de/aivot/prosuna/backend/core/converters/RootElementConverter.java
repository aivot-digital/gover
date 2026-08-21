package de.aivot.prosuna.backend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RootElementConverter implements AttributeConverter<FormLayoutElement, String> {
    @Override
    public String convertToDatabaseColumn(FormLayoutElement baseElement) {
        var mapper = JsonMapperFactory
                .getInstance();

        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FormLayoutElement convertToEntityAttribute(String s) {
        var mapper = JsonMapperFactory
                .getInstance();

        try {
            return mapper.readValue(s, FormLayoutElement.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
