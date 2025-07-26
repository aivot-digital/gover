package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.elements.models.elements.form.layout.GroupLayout;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GroupLayoutConverter implements AttributeConverter<GroupLayout, String> {
    @Override
    public String convertToDatabaseColumn(GroupLayout baseElement) {
        var mapper = new ObjectMapper();

        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GroupLayout convertToEntityAttribute(String s) {
        var mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            return mapper.readValue(s, GroupLayout.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
