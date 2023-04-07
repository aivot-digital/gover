package de.aivot.GoverBackend.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.models.elements.RootElement;
import org.json.JSONObject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class ElementConverter implements AttributeConverter<RootElement, String> {
    @Override
    public String convertToDatabaseColumn(RootElement baseElement) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RootElement convertToEntityAttribute(String s) {
        return new RootElement(new JSONObject(s).toMap());
    }
}
