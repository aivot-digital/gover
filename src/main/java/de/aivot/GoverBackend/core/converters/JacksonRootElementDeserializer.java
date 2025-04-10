package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import de.aivot.GoverBackend.elements.models.RootElement;

import java.util.Map;


public class JacksonRootElementDeserializer extends StdConverter<Map<String, Object>, RootElement> {
    @Override
    public RootElement convert(Map<String, Object> json) {
        return new RootElement(json);
    }
}
