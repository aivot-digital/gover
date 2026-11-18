package de.aivot.prosuna.backend.plugins.core.v1.operators.duration;

import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.prosuna.backend.utils.DurationValueUtils;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.*;

class NoCodeDurationOperatorsTest {
    @Test
    void publishesOneFormattingSignatureWithUnitSuggestions() throws Exception {
        var signatures = new NoCodeFormatDurationOperator().getSignatures();
        var mapper = JsonMapperFactory.getInstance();
        var json = mapper.readTree(mapper.writeValueAsString(signatures));
        assertEquals(1, json.size());
        assertEquals(2, json.get(0).get("parameters").size());
        var unit = json.get(0).get("parameters").get(1);
        assertEquals("Größte Einheit", unit.get("label").asText());
        assertEquals("hours", unit.get("options").get(0).get("value").asText());
        assertEquals("days", unit.get("options").get(1).get("value").asText());
    }

    @Test
    void createsDurationsInSecondsIncludingDecimalAmounts() throws NoCodeException {
        var operator = new NoCodeCreateDurationOperator();
        assertNumber(5400, operator.evaluate(runtime(), 1.5, "hours").getValue());
        assertNumber(1800, operator.evaluate(runtime(), 30, "minutes").getValue());
        assertNumber(75, operator.evaluate(runtime(), 75, "seconds").getValue());
        assertNumber(97200, operator.evaluate(runtime(), 27, " HOURS ").getValue());
        assertNumber(129600, operator.evaluate(runtime(), 1.5, "days").getValue());
        assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), DurationValueUtils.MAX_SECONDS, "days"));
        assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), 0.5, "seconds"));
        assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), -1, "hours"));
        assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), DurationValueUtils.MAX_SECONDS, "hours"));
        assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), 1, "months"));
    }

    @Test
    void convertsTotalsRatherThanIndividualParts() throws NoCodeException {
        var operator = new NoCodeConvertDurationOperator();
        assertNumber(2.5, operator.evaluate(runtime(), 9000, "hours").getValue());
        assertNumber(150, operator.evaluate(runtime(), 9000, "minutes").getValue());
        assertNumber(9000, operator.evaluate(runtime(), 9000, "seconds").getValue());
        assertNumber(0.01666667, operator.evaluate(runtime(), 1, "minutes").getValue());
        assertNumber(1.5, operator.evaluate(runtime(), 129600, "days").getValue());
        assertNumber(0.00001157, operator.evaluate(runtime(), 1, "days").getValue());
        assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), 1.5, "minutes"));
        assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), -1, "hours"));
    }

    @Test
    void formatsPluralizationZeroAndDurationsOverOneDay() throws NoCodeException {
        var operator = new NoCodeFormatDurationOperator();
        assertEquals("0 Sekunden", operator.evaluate(runtime(), 0, "hours").getValue());
        assertEquals("1 Stunde 1 Minute 1 Sekunde", operator.evaluate(runtime(), 3661, "hours").getValue());
        assertEquals("2 Stunden 30 Minuten", operator.evaluate(runtime(), 9000, "hours").getValue());
        assertEquals("27 Stunden", operator.evaluate(runtime(), 97200, "hours").getValue());
        assertEquals("27 Stunden", operator.evaluate(runtime(), 97200, null).getValue());
        assertEquals("27 Stunden", operator.evaluate(runtime(), 97200, "").getValue());
        assertEquals("1 Tag 3 Stunden 30 Minuten 15 Sekunden", operator.evaluate(runtime(), 99015, "days").getValue());
        assertEquals("2 Tage", operator.evaluate(runtime(), 172800, " DAYS ").getValue());
        assertEquals("0 Sekunden", operator.evaluate(runtime(), 0, "days").getValue());
        assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), 9000, "months"));
        assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), 9000, "minutes"));
        assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), -1, "hours"));
        assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), 0.5, "hours"));
    }

    @Test
    void retainsExistingNumericCoercionForEmptyValuesAndNumericStrings() throws NoCodeException {
        for (Object empty : new Object[]{null, ""}) {
            assertNumber(0, new NoCodeCreateDurationOperator().evaluate(runtime(), empty, "hours").getValue());
            assertNumber(0, new NoCodeConvertDurationOperator().evaluate(runtime(), empty, "hours").getValue());
            assertEquals("0 Sekunden", new NoCodeFormatDurationOperator().evaluate(runtime(), empty, "hours").getValue());
            assertEquals("0 Sekunden", new NoCodeFormatDurationOperator().evaluate(runtime(), empty, "days").getValue());
        }
        assertNumber(5400, new NoCodeCreateDurationOperator().evaluate(runtime(), "1.5", "hours").getValue());
    }

    @Test
    void registersOperatorsAndValidatesArgumentCounts() {
        var identifiers = Arrays.stream(new CommonOperatorsV1(null, null, null).getOperators())
                .map(operator -> operator.getIdentifier()).collect(Collectors.toSet());
        assertTrue(identifiers.containsAll(Set.of("create-duration", "convert-duration", "format-duration")));
        assertThrows(NoCodeException.class, () -> new NoCodeCreateDurationOperator().evaluate(runtime(), 1));
        assertThrows(NoCodeException.class, () -> new NoCodeFormatDurationOperator().evaluate(runtime()));
        assertThrows(NoCodeException.class, () -> new NoCodeFormatDurationOperator().evaluate(runtime(), 1));
        assertThrows(NoCodeException.class, () -> new NoCodeFormatDurationOperator().evaluate(runtime(), 1, "days", 2));
    }

    private static void assertNumber(double expected, Object actual) {
        assertEquals(0, BigDecimal.valueOf(expected).compareTo((BigDecimal) actual));
    }
}
