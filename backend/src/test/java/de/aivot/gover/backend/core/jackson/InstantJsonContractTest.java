package de.aivot.gover.backend.core.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import de.aivot.gover.backend.JacksonConfiguration;
import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

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
        assertRejectsOffsetlessInstant(ObjectMapperFactory.getInstance());
    }

    @Test
    void springMapperShouldRejectOffsetlessInstants() {
        assertRejectsOffsetlessInstant(springMapper());
    }

    @Test
    void objectMapperFactoryShouldCoerceEmptyInstantsToNull() throws Exception {
        assertCoercesEmptyInstantsToNull(ObjectMapperFactory.getInstance());
    }

    @Test
    void springMapperShouldCoerceEmptyInstantsToNull() throws Exception {
        assertCoercesEmptyInstantsToNull(springMapper());
    }

    @Test
    void shouldTreatUtcAndExplicitOffsetsAsTheSameInstant() throws Exception {
        var mapper = ObjectMapperFactory.getInstance();

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
        var mapper = ObjectMapperFactory.getInstance();

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
        var mapper = ObjectMapperFactory.getInstance();

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
        var mapper = ObjectMapperFactory.getInstance();

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
        var mapper = ObjectMapperFactory.getInstance();

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

        var result = ObjectMapperFactory
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

    private ObjectMapper springMapper() {
        var builder = Jackson2ObjectMapperBuilder.json();
        new JacksonConfiguration()
                .customJacksonSerializers()
                .customize(builder);
        return builder.build();
    }

    private record TimestampPayload(Instant timestamp) {
    }
}
