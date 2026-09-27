package de.aivot.prosuna.backend.core.converters;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;

@Converter
public class RootElementConverter implements AttributeConverter<FormLayoutElement, String> {
    @Override
    public String convertToDatabaseColumn(FormLayoutElement baseElement) {
        var mapper = JsonMapperFactory
                .getInstance();

        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FormLayoutElement convertToEntityAttribute(String s) {
        var mapper = JsonMapperFactory
                .getInstance();

        try {
            return mapper.readValue(s, FormLayoutElement.class);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
}
