package de.aivot.GoverBackend.core.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ObjectMapperFactory {
    private static ObjectMapper mapper;

    public static ObjectMapper getInstance() {
        if (mapper == null) {
            mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
        return mapper;
    }

    public static final class Utils {
        public static <T> List<T> convertToList(Object value, Class<T> elementType) {
            ObjectMapper om = getInstance();
            List<T> res = new LinkedList<>();

            if (value instanceof Collection<?> cValue) {
                for (Object itemObj : cValue) {
                    var conv = om.convertValue(itemObj, elementType);
                    res.add(conv);
                }
            }

            return res;
        }
    }
}
