package de.aivot.GoverBackend.core.jackson;

import de.aivot.GoverBackend.JacksonConfiguration;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FallbackZoneInstantDeserializerTest {
    private ZoneId originalZoneId;

    @BeforeEach
    void setUp() {
        originalZoneId = ApplicationTimeZone.getZoneId();
        ApplicationTimeZone.configure(ZoneId.of("Europe/Berlin"));
    }

    @AfterEach
    void tearDown() {
        ApplicationTimeZone.configure(originalZoneId);
    }

    @Test
    void objectMapperFactoryShouldAssumeApplicationTimeZoneForOffsetlessInstants() {
        var source = Map.of("timestamp", "2026-06-15T10:30:00");

        var payload = ObjectMapperFactory
                .getInstance()
                .convertValue(source, TimestampPayload.class);

        assertEquals(Instant.parse("2026-06-15T08:30:00Z"), payload.timestamp());
    }

    @Test
    void springMapperShouldAssumeApplicationTimeZoneForOffsetlessInstants() {
        var builder = Jackson2ObjectMapperBuilder.json();
        new JacksonConfiguration()
                .fallbackZoneInstantDeserializerCustomizer()
                .customize(builder);

        var mapper = builder.build();
        var source = Map.of("timestamp", "2026-06-15T10:30:00");

        var payload = mapper.convertValue(source, TimestampPayload.class);

        assertEquals(Instant.parse("2026-06-15T08:30:00Z"), payload.timestamp());
    }

    @Test
    void deserializerShouldKeepExplicitOffsetsAuthoritative() {
        var source = Map.of("timestamp", "2026-06-15T10:30:00+03:00");

        var payload = ObjectMapperFactory
                .getInstance()
                .convertValue(source, TimestampPayload.class);

        assertEquals(Instant.parse("2026-06-15T07:30:00Z"), payload.timestamp());
    }

    @Test
    void deserializerShouldKeepJacksonsNumericInstantHandling() {
        var source = Map.of("timestamp", 1770993000L);

        var payload = ObjectMapperFactory
                .getInstance()
                .convertValue(source, TimestampPayload.class);

        assertEquals(Instant.ofEpochSecond(1770993000L), payload.timestamp());
    }

    private record TimestampPayload(Instant timestamp) {
    }
}
