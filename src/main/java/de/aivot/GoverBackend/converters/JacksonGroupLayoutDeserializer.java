package de.aivot.GoverBackend.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import de.aivot.GoverBackend.models.elements.form.layout.GroupLayout;

import java.util.Map;


public class JacksonGroupLayoutDeserializer extends StdConverter<Map<String, Object>, GroupLayout> {
    @Override
    public GroupLayout convert(Map<String, Object> json) {
        return new GroupLayout(json);
    }
}
