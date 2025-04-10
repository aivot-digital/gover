package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;

import java.util.Map;


public class JacksonGroupLayoutDeserializer extends StdConverter<Map<String, Object>, GroupLayout> {
    @Override
    public GroupLayout convert(Map<String, Object> json) {
        return new GroupLayout(json);
    }
}
