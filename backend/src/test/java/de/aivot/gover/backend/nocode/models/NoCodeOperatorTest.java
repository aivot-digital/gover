package de.aivot.gover.backend.nocode.models;

import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.nocode.exceptions.NoCodeException;
import de.aivot.gover.backend.nocode.models.NoCodeOperator;
import de.aivot.gover.backend.nocode.models.NoCodeResult;
import de.aivot.gover.backend.nocode.models.NoCodeSignatur;
import de.aivot.gover.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoCodeOperatorTest {
    private static final ZoneId TEST_ZONE = ZoneId.of("Europe/Berlin");
    private ZoneId originalZone;

    private final NoCodeOperator operator = new NoCodeOperator() {
        @Override
        public String getIdentifier() {
            return "test-operator";
        }

        @Override
        public String getLabel() {
            return "Test Operator";
        }

        @Override
        public String getAbstract() {
            return "Abstract for Test Operator";
        }

        @Override
        public String getDescription() {
            return "Description for Test Operator";
        }

        @Override
        public NoCodeSignatur[] getSignatures() {
            return NoCodeSignatur.of();
        }

        @Override
        public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
            return null;
        }
    };

    @BeforeEach
    void configureApplicationTimeZone() {
        originalZone = ApplicationTimeZone.getZoneId();
        ApplicationTimeZone.configure(TEST_ZONE);
    }

    @AfterEach
    void restoreApplicationTimeZone() {
        ApplicationTimeZone.configure(originalZone);
    }

    @Test
    void getDeprecatedMessage() {
        assertNull(operator.getDeprecatedMessage());
    }

    @Test
    void castToTypeOfReference() {
        // Test casting to String
        assertEquals("123", operator.castToTypeOfReference("reference", 123));
        assertEquals("true", operator.castToTypeOfReference("reference", true));

        // Test casting to Integer
        assertEquals(123, operator.castToTypeOfReference(1, "123"));
        assertEquals(3, operator.castToTypeOfReference(1, "abc"));

        // Test casting to Float
        assertEquals(123.0f, operator.castToTypeOfReference(1.0f, "123"));
        assertEquals(3.0f, operator.castToTypeOfReference(1.0f, "abc"));

        // Test casting to Double
        assertEquals(123.0, operator.castToTypeOfReference(1.0, "123"));
        assertEquals(3.0, operator.castToTypeOfReference(1.0, "abc"));

        // Test casting to Long
        assertEquals(123L, operator.castToTypeOfReference(1L, "123"));
        assertEquals(3L, operator.castToTypeOfReference(1L, "abc"));

        // Test casting to BigDecimal
        assertEquals(new BigDecimal("123").setScale(8, RoundingMode.HALF_UP), operator.castToTypeOfReference(BigDecimal.ONE, "123"));
        assertEquals(new BigDecimal(3).setScale(8, RoundingMode.HALF_UP), operator.castToTypeOfReference(BigDecimal.ONE, "abc"));

        // Test casting to Boolean
        assertTrue((Boolean) operator.castToTypeOfReference(true, "true"));
        assertFalse((Boolean) operator.castToTypeOfReference(true, "false"));

        // Test casting to List
        assertEquals(List.of(1, 2, 3), operator.castToTypeOfReference(List.of(), "[1,2,3]"));
        assertEquals(List.of(), operator.castToTypeOfReference(List.of(), "invalid"));

        // Test casting to Map
        assertEquals(Map.of("key", "value"), operator.castToTypeOfReference(Map.of(), "{\"key\":\"value\"}"));
        assertEquals(Map.of(), operator.castToTypeOfReference(Map.of(), "invalid"));

        // Test casting to ZonedDateTime
        ZonedDateTime now = ZonedDateTime.now();
        assertEquals(
                now.toInstant().atZone(TEST_ZONE),
                operator.castToTypeOfReference(now, now.toOffsetDateTime().toString())
        );
    }

    @Test
    void castToBoolean() {
        assertTrue(operator.castToBoolean(true));
        assertFalse(operator.castToBoolean(false));
        assertTrue(operator.castToBoolean(1));
        assertFalse(operator.castToBoolean(0));
        assertTrue(operator.castToBoolean("true"));
        assertFalse(operator.castToBoolean("false"));
        assertFalse(operator.castToBoolean(""));
        assertTrue(operator.castToBoolean(List.of(1)));
        assertFalse(operator.castToBoolean(List.of()));
        assertTrue(operator.castToBoolean(Map.of("key", "value")));
        assertFalse(operator.castToBoolean(Map.of()));
    }

    @Test
    void castToNumber() {
        assertEquals(BigDecimal.valueOf(123).setScale(8, RoundingMode.HALF_UP), operator.castToNumber(123));
        assertEquals(BigDecimal.valueOf(123.0).setScale(8, RoundingMode.HALF_UP), operator.castToNumber(123.0));
        assertEquals(BigDecimal.valueOf(123.0f).setScale(8, RoundingMode.HALF_UP), operator.castToNumber(123.0f));
        assertEquals(BigDecimal.valueOf(123L).setScale(8, RoundingMode.HALF_UP), operator.castToNumber(123L));
        assertEquals(new BigDecimal("123").setScale(8, RoundingMode.HALF_UP), operator.castToNumber("123"));
        assertEquals(BigDecimal.valueOf(3).setScale(8, RoundingMode.HALF_UP), operator.castToNumber("abc"));
        assertEquals(BigDecimal.valueOf(1).setScale(8, RoundingMode.HALF_UP), operator.castToNumber(List.of(1)));
        assertEquals(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP), operator.castToNumber(List.of()));
        assertEquals(BigDecimal.valueOf(1).setScale(8, RoundingMode.HALF_UP), operator.castToNumber(Map.of("key", "value")));
        assertEquals(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP), operator.castToNumber(Map.of()));
    }

    @Test
    void castToString() {
        assertEquals("123", operator.castToString(123));
        assertEquals("123.0", operator.castToString(123.0));
        assertEquals("123.0", operator.castToString(123.0f));
        assertEquals("123", operator.castToString(123L));
        assertEquals("true", operator.castToString(true));
        assertEquals("[1,2,3]", operator.castToString(List.of(1, 2, 3)));
        assertEquals("{\"key\":\"value\"}", operator.castToString(Map.of("key", "value")));
        assertEquals(
                "2026-07-29T09:00:00+02:00",
                operator.castToString(Instant.parse("2026-07-29T07:00:00Z"))
        );
        assertEquals(
                "2026-07-29T09:00:00+02:00",
                operator.castToString(LocalDateTime.of(2026, 7, 29, 9, 0))
        );
        assertEquals(
                "[\"2026-07-29T09:00:00+02:00\"]",
                operator.castToString(List.of(Instant.parse("2026-07-29T07:00:00Z")))
        );
        assertEquals("2026-07", operator.castToString(YearMonth.of(2026, 7)));
        assertEquals("2026", operator.castToString(Year.of(2026)));
        assertEquals("09:30:00", operator.castToString(LocalTime.of(9, 30)));
        assertEquals("09:30:15", operator.castToString(LocalTime.of(9, 30, 15, 999_000_000)));
    }

    @Test
    void castsPartialDatePrecisionsThroughTheirFirstRepresentableDay() {
        assertEquals(
                LocalDate.of(2026, 7, 1),
                operator.castToDate(YearMonth.of(2026, 7))
        );
        assertEquals(
                LocalDate.of(2026, 1, 1),
                operator.castToDate(Year.of(2026))
        );
        assertEquals(
                YearMonth.of(2026, 8),
                operator.castToTypeOfReference(YearMonth.of(2026, 7), "2026-08")
        );
        assertEquals(
                Year.of(2027),
                operator.castToTypeOfReference(Year.of(2026), "2027")
        );
    }

    @Test
    void castToDateTime() {
        var utcDateTime = ZonedDateTime.of(
                LocalDateTime.of(2026, 8, 3, 12, 0),
                ZoneOffset.UTC
        );
        assertEquals(
                utcDateTime.withZoneSameInstant(TEST_ZONE),
                operator.castToDateTime(utcDateTime)
        );
        assertEquals(
                Instant.parse("2023-01-01T00:00:00Z"),
                operator.castToDateTime("2023-01-01T00:00:00Z").toInstant()
        );
        assertEquals(
                ZoneOffset.ofHours(2),
                operator.castToDateTime(LocalDateTime.of(2026, 10, 25, 2, 30)).getOffset()
        );
        assertEquals(
                BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP),
                operator.castToNumber(LocalDateTime.of(2026, 3, 29, 2, 30))
        );
        assertDoesNotThrow(() -> operator.castToDateTime("invalid"));
        assertDoesNotThrow(() -> operator.castToTypeOfReference(utcDateTime, "invalid"));
    }

    @Test
    void castToMap() {
        assertEquals(Map.of("key", "value"), operator.castToMap(Map.of("key", "value")));
        assertEquals(Map.of("key", "value"), operator.castToMap("{\"key\":\"value\"}"));
        assertEquals(Map.of(), operator.castToMap("invalid"));
    }

    @Test
    void castToList() {
        assertEquals(List.of(1, 2, 3), operator.castToList(List.of(1, 2, 3)));
        assertEquals(List.of(1, 2, 3), operator.castToList("[1,2,3]"));
        assertEquals(List.of(), operator.castToList("invalid"));
    }
}
