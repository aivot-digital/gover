package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
        mapper.registerModule(new JavaTimeModule());

        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ElementData convertToEntityAttribute(String s) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        try {
            return mapper.readValue(s, ElementData.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public ElementData convertToEntityAttribute(Map<?, ?> m) {
        return convertObjectToEntityAttribute(m);
    }

    public ElementData convertObjectToEntityAttribute(Object o) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        if (o instanceof Map<?, ?> map) {
            return convertToEntityAttribute(map);
        } else if (o instanceof String jsonString) {
            try {
                return mapper.readValue(jsonString, ElementData.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported type for conversion: " + o.getClass().getName());
        }
    }
}
