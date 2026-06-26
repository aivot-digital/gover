package de.aivot.gover.backend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.elements.models.elements.layout.GroupLayoutElement;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GroupLayoutConverter implements AttributeConverter<GroupLayoutElement, String> {
    @Override
    public String convertToDatabaseColumn(GroupLayoutElement baseElement) {
        var mapper = ObjectMapperFactory
                .getInstance();

        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GroupLayoutElement convertToEntityAttribute(String s) {
        var mapper = ObjectMapperFactory
                .getInstance();

        try {
            return mapper.readValue(s, GroupLayoutElement.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
