package de.aivot.GoverBackend.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdConverter;
import de.aivot.GoverBackend.models.elements.RootElement;

import java.util.Map;


public class JacksonRootElementSerializer extends StdConverter<RootElement, Map<String, Object>> {
    @Override
    public Map<String, Object> convert(RootElement e) {
        var mapper = new ObjectMapper();
        return mapper.convertValue(e, Map.class);
    }
}
