package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.elements.models.ElementData;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.json.JSONObject;

import java.util.Map;

@Converter
public class ElementDataConverter implements AttributeConverter<ElementData, String> {
    @Override
    public String convertToDatabaseColumn(ElementData baseElement) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ElementData convertToEntityAttribute(String s) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(s, ElementData.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
