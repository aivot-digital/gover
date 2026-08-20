package de.aivot.prosuna.backend.core.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.prosuna.backend.JacksonConfiguration;
import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalTimeJsonContractTest {
    @Test
    void objectMapperFactoryShouldSerializeLocalTimeWithSeconds() throws Exception {
        assertSerializesWithSeconds(ObjectMapperFactory.getInstance());
    }

    @Test
    void springMapperShouldSerializeLocalTimeWithSeconds() throws Exception {
        assertSerializesWithSeconds(springMapper());
    }

    @Test
    void shouldAcceptMinuteAndSecondInput() throws Exception {
        var mapper = ObjectMapperFactory.getInstance();

        assertEquals(
                LocalTime.of(9, 30),
                mapper.readValue("{\"time\":\"09:30\"}", TimePayload.class).time()
        );
        assertEquals(
                LocalTime.of(9, 30, 15),
                mapper.readValue("{\"time\":\"09:30:15\"}", TimePayload.class).time()
        );
    }

    private void assertSerializesWithSeconds(ObjectMapper mapper) throws Exception {
        assertEquals(
                "{\"time\":\"09:30:00\"}",
                mapper.writeValueAsString(new TimePayload(LocalTime.of(9, 30)))
        );
        assertEquals(
                "{\"time\":\"09:30:15\"}",
                mapper.writeValueAsString(new TimePayload(LocalTime.of(9, 30, 15, 123_000_000)))
        );
    }

    private ObjectMapper springMapper() {
        var builder = Jackson2ObjectMapperBuilder.json();
        new JacksonConfiguration()
                .customJacksonSerializers()
                .customize(builder);
        return builder.build();
    }

    private record TimePayload(LocalTime time) {
    }
}
