package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdConverter;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;

import java.util.Map;


public class JacksonGroupLayoutSerializer extends StdConverter<GroupLayout, Map<String, Object>> {
    @Override
    public Map<String, Object> convert(GroupLayout e) {
        var mapper = new ObjectMapper();
        return mapper.convertValue(e, Map.class);
    }
}
