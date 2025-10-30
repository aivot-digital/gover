package de.aivot.GoverBackend.core.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.aivot.GoverBackend.core.converters.ElementDataObjectDeserializer;
import de.aivot.GoverBackend.elements.models.ElementDataObject;

public class ObjectMapperFactory {
    private static ObjectMapper mapper;

    public static ObjectMapper getInstance() {
        if (mapper == null) {
            var goverDeserializers = new SimpleModule();
            goverDeserializers
                    .addDeserializer(ElementDataObject.class, new ElementDataObjectDeserializer());

            mapper =  new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .registerModule(goverDeserializers)
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
        return mapper;
    }
}
