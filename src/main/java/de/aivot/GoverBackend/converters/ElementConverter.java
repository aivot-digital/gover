package de.aivot.GoverBackend.converters;

import org.json.JSONObject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Map;

@Converter
public class ElementConverter implements AttributeConverter<Map<String, Object>, String> {
    @Override
    public String convertToDatabaseColumn(Map<String, Object> baseElement) {
        JSONObject json = new JSONObject(baseElement);
        return json.toString();
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String s) {
        JSONObject json = new JSONObject(s);
        return json.toMap();
    }
}
