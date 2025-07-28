package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.aivot.GoverBackend.elements.models.ElementData;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter
public class ElementDataConverter implements AttributeConverter<ElementData, String> {
    @Override
    public String convertToDatabaseColumn(ElementData baseElement) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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

    public ElementData convertToEntityAttribute(Map<?, ?> map) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        try {
            return mapper.convertValue(map, ElementData.class);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    public ElementData convertObjectToEntityAttribute(Object o) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        if (o instanceof Map<?, ?> map) {
            return convertToEntityAttribute(map);
        } else if (o instanceof String string) {
            return convertToEntityAttribute(string);
        } else {
            throw new IllegalArgumentException("Unsupported type for conversion: " + o.getClass().getName());
        }
    }
}
