package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.ElementData;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter
public class ElementDataConverter implements AttributeConverter<ElementData, String> {


    @Override
    public String convertToDatabaseColumn(ElementData baseElement) {
        var mapper = ObjectMapperFactory
                .getInstance();

        try {
            return mapper.writeValueAsString(baseElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ElementData convertToEntityAttribute(String s) {
        var mapper =  ObjectMapperFactory
                .getInstance();

        try {
            return mapper.readValue(s, ElementData.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public ElementData convertToEntityAttribute(Map<?, ?> map) {
        var mapper = ObjectMapperFactory
                .getInstance();

        try {
            return mapper.convertValue(map, ElementData.class);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    public ElementData convertObjectToEntityAttribute(Object o) {
        if (o instanceof Map<?, ?> map) {
            return convertToEntityAttribute(map);
        } else if (o instanceof String string) {
            return convertToEntityAttribute(string);
        } else {
            throw new IllegalArgumentException("Unsupported type for conversion: " + o.getClass().getName());
        }
    }
}
