package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.elements.models.RootElement;
import org.json.JSONObject;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RootElementConverter implements AttributeConverter<RootElement, String> {
    @Override
    public String convertToDatabaseColumn(RootElement baseElement) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            var res = mapper.writeValueAsString(baseElement);
            return res;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RootElement convertToEntityAttribute(String s) {
        var elem = new RootElement(new JSONObject(s).toMap());
        return elem;
    }
}
