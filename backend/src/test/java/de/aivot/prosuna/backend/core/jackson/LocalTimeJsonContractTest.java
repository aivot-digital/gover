package de.aivot.prosuna.backend.core.jackson;

import de.aivot.prosuna.backend.JsonMapperConfiguration;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalTimeJsonContractTest {
    @Test
    void objectMapperFactoryShouldSerializeLocalTimeWithSeconds() throws Exception {
        assertSerializesWithSeconds(JsonMapperFactory.getInstance());
    }

    @Test
    void springMapperShouldSerializeLocalTimeWithSeconds() throws Exception {
        var mapper = JsonMapperFactory.getInstance();

        assertEquals(
                "{\"time\":\"09:30:00\"}",
                mapper.writeValueAsString(new TimePayload(LocalTime.of(9, 30)))
        );
        assertEquals(
                "{\"time\":\"09:30:15\"}",
                mapper.writeValueAsString(new TimePayload(LocalTime.of(9, 30, 15, 123_000_000)))
        );
    }

    @Test
    void shouldAcceptMinuteAndSecondInput() throws Exception {
        var mapper = springMapper();

        assertEquals(
                LocalTime.of(9, 30),
                mapper.readValue("{\"time\":\"09:30\"}", TimePayload.class).time()
        );
        assertEquals(
                LocalTime.of(9, 30, 15),
                mapper.readValue("{\"time\":\"09:30:15\"}", TimePayload.class).time()
        );
    }

    private void assertSerializesWithSeconds(JsonMapper mapper) throws Exception {
        assertEquals(
                "{\"time\":\"09:30:00\"}",
                mapper.writeValueAsString(new TimePayload(LocalTime.of(9, 30)))
        );
        assertEquals(
                "{\"time\":\"09:30:15\"}",
                mapper.writeValueAsString(new TimePayload(LocalTime.of(9, 30, 15, 123_000_000)))
        );
    }

    private JsonMapper springMapper() {
        var builder = JsonMapper.builder();
        new JsonMapperConfiguration()
                .customJacksonSerializers()
                .customize(builder);
        return builder.build();
    }

    private record TimePayload(LocalTime time) {
    }
}
