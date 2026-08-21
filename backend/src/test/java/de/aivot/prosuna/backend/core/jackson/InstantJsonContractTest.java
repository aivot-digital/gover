package de.aivot.prosuna.backend.core.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import de.aivot.prosuna.backend.JacksonConfiguration;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstantJsonContractTest {
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
    void objectMapperFactoryShouldRejectOffsetlessInstants() {
        assertRejectsOffsetlessInstant(JsonMapperFactory.getInstance());
    }

    @Test
    void springMapperShouldRejectOffsetlessInstants() {
        assertThrows(
                tools.jackson.databind.exc.MismatchedInputException.class,
                () -> springMapper().readValue(
                        "{\"timestamp\":\"2026-06-15T10:30:00\"}",
                        TimestampPayload.class
                )
        );
    }

    @Test
    void objectMapperFactoryShouldCoerceEmptyInstantsToNull() throws Exception {
        assertCoercesEmptyInstantsToNull(JsonMapperFactory.getInstance());
    }

    @Test
    void springMapperShouldCoerceEmptyInstantsToNull() throws Exception {
        var mapper = springMapper();

        assertNull(mapper.readValue(
                "{\"timestamp\":\"\"}",
                TimestampPayload.class
        ).timestamp());
        assertNull(mapper.readValue(
                "{\"timestamp\":\"   \"}",
                TimestampPayload.class
        ).timestamp());
    }

    @Test
    void shouldTreatUtcAndExplicitOffsetsAsTheSameInstant() throws Exception {
        var mapper = JsonMapperFactory.getInstance();

        var utcPayload = mapper.readValue(
                "{\"timestamp\":\"2026-06-15T07:30:00Z\"}",
                TimestampPayload.class
        );
        var offsetPayload = mapper.readValue(
                "{\"timestamp\":\"2026-06-15T10:30:00+03:00\"}",
                TimestampPayload.class
        );

        assertEquals(utcPayload.timestamp(), offsetPayload.timestamp());
        assertEquals(Instant.parse("2026-06-15T07:30:00Z"), offsetPayload.timestamp());
    }

    @Test
    void shouldRejectNumericInstants() {
        var mapper = JsonMapperFactory.getInstance();

        assertThrows(
                MismatchedInputException.class,
                () -> mapper.readValue(
                        "{\"timestamp\":1770993000}",
                        TimestampPayload.class
                )
        );
    }

    @Test
    void shouldRejectInstantsWithoutSeconds() {
        var mapper = JsonMapperFactory.getInstance();

        assertThrows(
                MismatchedInputException.class,
                () -> mapper.readValue(
                        "{\"timestamp\":\"2026-06-15T10:30+03:00\"}",
                        TimestampPayload.class
                )
        );
    }

    @Test
    void shouldRejectNonCanonicalEndOfDayNotation() {
        var mapper = JsonMapperFactory.getInstance();

        assertThrows(
                MismatchedInputException.class,
                () -> mapper.readValue(
                        "{\"timestamp\":\"2026-06-15T24:00:00+02:00\"}",
                        TimestampPayload.class
                )
        );
    }

    @Test
    void shouldSerializeSummerAndWinterInstantsWithApplicationTimeZoneOffset() throws Exception {
        var mapper = JsonMapperFactory.getInstance();

        assertEquals(
                "{\"timestamp\":\"2026-06-15T09:30:00+02:00\"}",
                mapper.writeValueAsString(
                        new TimestampPayload(Instant.parse("2026-06-15T07:30:00Z"))
                )
        );
        assertEquals(
                "{\"timestamp\":\"2026-01-15T08:30:00+01:00\"}",
                mapper.writeValueAsString(
                        new TimestampPayload(Instant.parse("2026-01-15T07:30:00Z"))
                )
        );
    }

    @Test
    void springMapperShouldSerializeInstantsWithApplicationTimeZoneOffset() throws Exception {
        var result = springMapper().writeValueAsString(
                new TimestampPayload(Instant.parse("2026-06-15T07:30:00Z"))
        );

        assertEquals("{\"timestamp\":\"2026-06-15T09:30:00+02:00\"}", result);
    }

    @Test
    void shouldUseNumericOffsetForUtcApplicationTimeZone() throws Exception {
        ApplicationTimeZone.configure(ZoneId.of("UTC"));

        var result = JsonMapperFactory
                .getInstance()
                .writeValueAsString(
                        new TimestampPayload(Instant.parse("2026-06-15T07:30:00Z"))
                );

        assertEquals("{\"timestamp\":\"2026-06-15T07:30:00+00:00\"}", result);
    }

    private void assertRejectsOffsetlessInstant(ObjectMapper mapper) {
        assertThrows(
                MismatchedInputException.class,
                () -> mapper.readValue(
                        "{\"timestamp\":\"2026-06-15T10:30:00\"}",
                        TimestampPayload.class
                )
        );
    }

    private void assertCoercesEmptyInstantsToNull(ObjectMapper mapper) throws Exception {
        assertNull(mapper.readValue(
                "{\"timestamp\":\"\"}",
                TimestampPayload.class
        ).timestamp());
        assertNull(mapper.readValue(
                "{\"timestamp\":\"   \"}",
                TimestampPayload.class
        ).timestamp());
    }

    private JsonMapper springMapper() {
        var builder = JsonMapper.builder();
        new JacksonConfiguration()
                .customJacksonSerializers()
                .customize(builder);
        return builder.build();
    }

    private record TimestampPayload(Instant timestamp) {
    }
}
