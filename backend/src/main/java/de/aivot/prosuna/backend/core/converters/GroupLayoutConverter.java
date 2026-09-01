package de.aivot.prosuna.backend.core.converters;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;

@Converter
public class GroupLayoutConverter implements AttributeConverter<GroupLayoutElement, String> {
    @Override
    public String convertToDatabaseColumn(GroupLayoutElement baseElement) {
        var mapper = JsonMapperFactory
                .getInstance();

        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GroupLayoutElement convertToEntityAttribute(String s) {
        var mapper = JsonMapperFactory
                .getInstance();

        try {
            return mapper.readValue(s, GroupLayoutElement.class);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
}
