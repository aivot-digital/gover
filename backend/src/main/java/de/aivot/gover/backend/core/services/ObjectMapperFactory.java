package de.aivot.gover.backend.core.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import de.aivot.gover.backend.core.jackson.ApplicationTimeZoneInstantSerializer;
import de.aivot.gover.backend.core.jackson.DurationToMillisecondsSerializer;
import de.aivot.gover.backend.core.jackson.StrictInstantDeserializer;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ObjectMapperFactory {
    private static ObjectMapper mapper;
    private static ObjectMapper nullPreservingMapper;

    public static ObjectMapper getInstance() {
        if (mapper == null) {
            mapper = new ObjectMapper()
                    .registerModule(createJavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
        return mapper;
    }

    public static ObjectMapper getNullPreservingInstance() {
        if (nullPreservingMapper == null) {
            nullPreservingMapper = getInstance()
                    .copy()
                    .setSerializationInclusion(JsonInclude.Include.ALWAYS);
        }
        return nullPreservingMapper;
    }

    private static JavaTimeModule createJavaTimeModule() {
        // These mappers are created outside Spring Boot and therefore do not inherit
        // JacksonConfiguration. Keep their temporal wire contract explicitly aligned
        // with the HTTP ObjectMapper.
        var module = new JavaTimeModule();
        module.addDeserializer(Instant.class, new StrictInstantDeserializer());
        module.addSerializer(Instant.class, new ApplicationTimeZoneInstantSerializer());
        // The input precision must not alter the wire contract. Jackson's default ISO
        // representation is therefore replaced with a stable second-precision format.
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        module.addSerializer(Duration.class, new DurationToMillisecondsSerializer());
        return module;
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

        public static Map<String, Object> convertToMap(Object value) {
            ObjectMapper om = getInstance();
            return om.convertValue(value, Map.class);
        }

        public static Map<String, Object> convertToMapPreservingNulls(Object value) {
            ObjectMapper om = getNullPreservingInstance();
            return om.convertValue(value, Map.class);
        }
    }
}
