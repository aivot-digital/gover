package de.aivot.GoverBackend.nocode.models;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoCodeOperatorTest {

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
        public NoCodeParameter[] getParameters() {
            return new NoCodeParameter[0];
        }

        @Override
        public NoCodeDataType getReturnType() {
            return NoCodeDataType.String;
        }

        @Override
        public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
            return null;
        }
    };

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
        assertEquals(0, operator.castToTypeOfReference(1, "abc"));

        // Test casting to Float
        assertEquals(123.0f, operator.castToTypeOfReference(1.0f, "123"));
        assertEquals(0.0f, operator.castToTypeOfReference(1.0f, "abc"));

        // Test casting to Double
        assertEquals(123.0, operator.castToTypeOfReference(1.0, "123"));
        assertEquals(0.0, operator.castToTypeOfReference(1.0, "abc"));

        // Test casting to Long
        assertEquals(123L, operator.castToTypeOfReference(1L, "123"));
        assertEquals(0L, operator.castToTypeOfReference(1L, "abc"));

        // Test casting to BigDecimal
        assertEquals(new BigDecimal("123"), operator.castToTypeOfReference(BigDecimal.ONE, "123"));
        assertEquals(BigDecimal.ZERO, operator.castToTypeOfReference(BigDecimal.ONE, "abc"));

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
        assertEquals(now, operator.castToTypeOfReference(now, now.toString()));
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
        assertEquals(BigDecimal.valueOf(123), operator.castToNumber(123));
        assertEquals(BigDecimal.valueOf(123.0), operator.castToNumber(123.0));
        assertEquals(BigDecimal.valueOf(123.0f), operator.castToNumber(123.0f));
        assertEquals(BigDecimal.valueOf(123L), operator.castToNumber(123L));
        assertEquals(new BigDecimal("123"), operator.castToNumber("123"));
        assertEquals(BigDecimal.ZERO, operator.castToNumber("abc"));
        assertEquals(BigDecimal.valueOf(1), operator.castToNumber(List.of(1)));
        assertEquals(BigDecimal.ZERO, operator.castToNumber(List.of()));
        assertEquals(BigDecimal.valueOf(1), operator.castToNumber(Map.of("key", "value")));
        assertEquals(BigDecimal.ZERO, operator.castToNumber(Map.of()));
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
    }

    @Test
    void castToDateTime() {
        ZonedDateTime now = ZonedDateTime.now();
        assertEquals(now, operator.castToDateTime(now));
        assertEquals(ZonedDateTime.parse("2023-01-01T00:00:00Z"), operator.castToDateTime("2023-01-01T00:00:00Z"));
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