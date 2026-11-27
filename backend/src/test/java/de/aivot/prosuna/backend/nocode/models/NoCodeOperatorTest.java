package de.aivot.prosuna.backend.nocode.models;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
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

        // Test casting to numbers. Text that is not a number cannot be cast.
        assertEquals(123, operator.castToTypeOfReference(1, "123"));
        assertNull(operator.castToTypeOfReference(1, "abc"));
        assertEquals(123.0f, operator.castToTypeOfReference(1.0f, "123"));
        assertEquals(123.0, operator.castToTypeOfReference(1.0, "123"));
        assertEquals(123L, operator.castToTypeOfReference(1L, "123"));
        assertEquals(new BigDecimal("123"), operator.castToTypeOfReference(BigDecimal.ONE, "123"));
        assertNull(operator.castToTypeOfReference(BigDecimal.ONE, "abc"));

        // Test casting to Boolean
        assertTrue((Boolean) operator.castToTypeOfReference(true, "true"));
        assertFalse((Boolean) operator.castToTypeOfReference(true, "false"));
        assertNull(operator.castToTypeOfReference(true, "Nein"));

        // Test casting to List and Map
        assertEquals(List.of(1, 2, 3), operator.castToTypeOfReference(List.of(), "[1,2,3]"));
        assertNull(operator.castToTypeOfReference(List.of(), "invalid"));
        assertEquals(Map.of("key", "value"), operator.castToTypeOfReference(Map.of(), "{\"key\":\"value\"}"));
        assertNull(operator.castToTypeOfReference(Map.of(), "invalid"));

        // Test casting to ZonedDateTime
        ZonedDateTime now = ZonedDateTime.now();
        assertEquals(
                now.toInstant().atZone(TEST_ZONE),
                operator.castToTypeOfReference(now, now.toOffsetDateTime().toString())
        );
        assertNull(operator.castToTypeOfReference(now, "invalid"));
    }

    @Test
    void isEqualToReferenceComparesNumbersByValue() {
        // Number fields keep their decimal places, e.g. 5 or 5.50.
        assertTrue(operator.isEqualToReference(new BigDecimal("5"), "5"));
        // Constants are stored as text and may precede the number field.
        assertTrue(operator.isEqualToReference("5.5", new BigDecimal("5.50")));
        assertTrue(operator.isEqualToReference("05", 5));
        assertTrue(operator.isEqualToReference(new BigDecimal("5.50"), "5.5"));
        assertTrue(operator.isEqualToReference(new BigDecimal("5.50"), 5.5));
        assertTrue(operator.isEqualToReference(5, new BigDecimal("5.00")));
        assertTrue(operator.isEqualToReference("5,5", new BigDecimal("5.5")));
        assertFalse(operator.isEqualToReference(new BigDecimal("5"), "6"));
        assertFalse(operator.isEqualToReference(5, "5.5"));

        // Two texts stay texts.
        assertFalse(operator.isEqualToReference("5.0", "5"));
        assertTrue(operator.isEqualToReference("a", "a"));
        assertFalse(operator.isEqualToReference("a", "b"));
        assertTrue(operator.isEqualToReference(true, "true"));
    }

    @Test
    void isEqualToReferenceDoesNotEqualValuesThatCannotBeCast() {
        // Text is not compared by its length, in either order.
        assertFalse(operator.isEqualToReference(3, "abc"));
        assertFalse(operator.isEqualToReference("abc", 3));
        // Conditions are not numbers.
        assertFalse(operator.isEqualToReference(0, true));
        assertFalse(operator.isEqualToReference(true, 0));
        // Invalid text does not become an empty list or object, today or the current time.
        assertFalse(operator.isEqualToReference(List.of(), "kein JSON"));
        assertFalse(operator.isEqualToReference(Map.of(), "kein JSON"));
        assertFalse(operator.isEqualToReference(LocalDate.now(TEST_ZONE), "kein Datum"));
        assertFalse(operator.isEqualToReference(LocalTime.now(TEST_ZONE).withNano(0), "keine Uhrzeit"));
    }

    @Test
    void isEqualToReferenceDoesNotExpandHugeNumbersInText() {
        // Rescaling such text fails or computes an enormous number.
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertFalse(operator.isEqualToReference("1e999999999", new BigDecimal("5")));
            assertFalse(operator.isEqualToReference(new BigDecimal("5"), "1e-999999999"));
            assertFalse(operator.isEqualToReference(new BigDecimal("5"), "1e300000000"));
        });
    }

    @Test
    void castToBoolean() throws NoCodeException {
        assertTrue(NoCodeOperator.castToBoolean(true));
        assertFalse(NoCodeOperator.castToBoolean(false));
        assertFalse(NoCodeOperator.castToBoolean(null));
        assertTrue(NoCodeOperator.castToBoolean("true"));
        assertTrue(NoCodeOperator.castToBoolean(" Wahr "));
        assertFalse(NoCodeOperator.castToBoolean("false"));
        assertFalse(NoCodeOperator.castToBoolean(" false"));
        assertFalse(NoCodeOperator.castToBoolean("FALSCH"));
        assertFalse(NoCodeOperator.castToBoolean(""));
        assertFalse(NoCodeOperator.castToBoolean("   "));
    }

    @Test
    void castToBooleanRejectsOtherValues() {
        for (var value : List.of("0", "Nein", "ja", 0, 1, BigDecimal.ONE, List.of(1), Map.of("key", "value"))) {
            var exception = assertThrows(NoCodeException.class, () -> NoCodeOperator.castToBoolean(value), String.valueOf(value));
            assertEquals("Der Wert ist weder wahr noch falsch. Verwenden Sie eine Bedingung oder einen Wahrheitswert.", exception.getMessage());
        }
    }

    @Test
    void castToNumber() throws NoCodeException {
        assertEquals(BigDecimal.valueOf(123), operator.castToNumber(123));
        assertEquals(BigDecimal.valueOf(123.0), operator.castToNumber(123.0));
        assertEquals(new BigDecimal("123.0"), operator.castToNumber(123.0f));
        assertEquals(BigDecimal.valueOf(123L), operator.castToNumber(123L));
        assertEquals(new BigDecimal("123"), operator.castToNumber(" 123 "));
        assertEquals(new BigDecimal("1.5"), operator.castToNumber("1,5"));
        assertEquals(new BigDecimal("-1.5"), operator.castToNumber("-1.5"));
        // Inputs keep their precision.
        assertEquals(new BigDecimal("0.123456789"), operator.castToNumber("0.123456789"));
        // Missing values are 0, so optional fields can be used in calculations.
        assertEquals(BigDecimal.ZERO, operator.castToNumber(null));
        assertEquals(BigDecimal.ZERO, operator.castToNumber(" "));
    }

    @Test
    void castToNumberRejectsOtherValues() {
        for (var value : List.of("abc", "1.234,5", true, List.of(1), Map.of("key", "value"), LocalDate.of(2026, 1, 1), "2026-01-01", "12:30")) {
            var exception = assertThrows(NoCodeException.class, () -> operator.castToNumber(value), String.valueOf(value));
            assertEquals("Der Wert ist keine Zahl. Verwenden Sie eine Zahl wie 1.5 oder 1,5.", exception.getMessage());
        }
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertThrows(NoCodeException.class, () -> operator.castToNumber("1e999999999"));
            assertThrows(NoCodeException.class, () -> operator.castToNumber("1e-999999999"));
            // Precision minus scale overflows int for this exponent.
            assertThrows(NoCodeException.class, () -> operator.castToNumber("1e2147483647"));
        });
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
    void castsPartialDatePrecisionsThroughTheirFirstRepresentableDay() throws NoCodeException {
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
    void castToDateTime() throws NoCodeException {
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
        // Values without a valid point in time are rejected instead of becoming the current one.
        assertThrows(NoCodeException.class, () -> operator.castToDateTime(LocalDateTime.of(2026, 3, 29, 2, 30)));
        assertThrows(NoCodeException.class, () -> operator.castToDateTime("invalid"));
        assertThrows(NoCodeException.class, () -> operator.castToDateTime(null));
        assertThrows(NoCodeException.class, () -> operator.castToDate("invalid"));
        assertThrows(NoCodeException.class, () -> operator.castToTime("invalid"));
    }

    @Test
    void castToTimeReadsStoredTimestampsInTheApplicationTimeZone() throws NoCodeException {
        assertEquals(LocalTime.of(13, 30), operator.castToTime("2026-11-22T12:30:00Z"));
        assertEquals(LocalTime.of(13, 30), operator.castToTime(Instant.parse("2026-11-22T12:30:00Z")));
        assertEquals(LocalTime.of(9, 15), operator.castToTime("09:15"));
    }

    @Test
    void castToMap() throws NoCodeException {
        assertEquals(Map.of("key", "value"), operator.castToMap(Map.of("key", "value")));
        assertEquals(Map.of("key", "value"), operator.castToMap("{\"key\":\"value\"}"));
        assertEquals(Map.of(), operator.castToMap(null));
        assertEquals(Map.of(), operator.castToMap(" "));
        assertNull(operator.castToMap("null"));
        assertFalse(operator.isEqualToReference(Map.of(), "null"));
        for (var value : List.of("invalid", "[1]", 1, true, List.of())) {
            var exception = assertThrows(NoCodeException.class, () -> operator.castToMap(value), String.valueOf(value));
            assertEquals("Der Wert ist kein Objekt.", exception.getMessage());
        }
    }

    @Test
    void castToList() throws NoCodeException {
        assertEquals(List.of(1, 2, 3), operator.castToList(List.of(1, 2, 3)));
        assertEquals(List.of(1, 2, 3), operator.castToList("[1,2,3]"));
        assertEquals(List.of(), operator.castToList(null));
        assertEquals(List.of(), operator.castToList(" "));
        assertNull(operator.castToList("null"));
        assertFalse(operator.isEqualToReference(List.of(), "null"));
        for (var value : List.of("invalid", "{}", 1, true, Map.of())) {
            var exception = assertThrows(NoCodeException.class, () -> operator.castToList(value), String.valueOf(value));
            assertEquals("Der Wert ist keine Liste.", exception.getMessage());
        }
    }
}
