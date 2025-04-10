package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import org.json.JSONObject;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GroupLayoutConverter implements AttributeConverter<GroupLayout, String> {
    @Override
    public String convertToDatabaseColumn(GroupLayout baseElement) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GroupLayout convertToEntityAttribute(String s) {
        return new GroupLayout(new JSONObject(s).toMap());
    }
}
