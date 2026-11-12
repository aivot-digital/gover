package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.DurationPrecision;
import de.aivot.prosuna.backend.enums.DurationLargestUnit;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.utils.DurationValueUtils;
import de.aivot.prosuna.backend.ozgCloud.services.OZGCloudDataFormatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static de.aivot.prosuna.backend.TestData.runtime;

class DurationInputElementTest {
    @Test
    void displaysAndValidatesDaysAsFixed24HourUnits() throws Exception {
        var field = new DurationInputElement().setLargestUnit(DurationLargestUnit.Day)
                .setMinDuration(86400L).setMaxDuration(172800L);
        assertEquals("1 Tag", field.toDisplayValue(BigDecimal.valueOf(86400)));
        assertEquals("2 Tage", field.toDisplayValue(BigDecimal.valueOf(172800)));
        assertEquals("1 Tag 3 Stunden 30 Minuten", field.toDisplayValue(BigDecimal.valueOf(99000)));
        field.validate(86400);
        field.validate(172800);
        assertEquals("Die Zeitdauer muss mindestens 1 Tag betragen.",
                assertThrows(ValidationException.class, () -> field.validate(0)).getMessage());
        assertEquals("Die Zeitdauer darf höchstens 2 Tage betragen.",
                assertThrows(ValidationException.class, () -> field.validate(259200)).getMessage());
        assertThrows(ValidationException.class, () -> field.validate(99015));
        field.setPrecision(DurationPrecision.Second);
        field.validate(99015);
        assertEquals("1 Tag 3 Stunden 30 Minuten 15 Sekunden", field.toDisplayValue(BigDecimal.valueOf(99015)));
        field.setLargestUnit(DurationLargestUnit.Hour);
        assertEquals("27 Stunden 30 Minuten 15 Sekunden", field.toDisplayValue(BigDecimal.valueOf(99015)));
    }

    @Test
    void exportsADurationLabelToOzgCloud() {
        var field = new DurationInputElement();
        field.setId("duration");
        field.setLabel("Aufwand");
        var exported = new OZGCloudDataFormatService().buildFormData(
                field, new AuthoredElementValues().putLiteral("duration", 99000), runtime("duration", 99000));
        assertEquals(1, exported.size());
        assertEquals("Aufwand", exported.getFirst().label());
        assertEquals("27 Stunden 30 Minuten", exported.getFirst().stringValue());
        field.setLargestUnit(DurationLargestUnit.Day);
        var exportedDays = new OZGCloudDataFormatService().buildFormData(
                field, new AuthoredElementValues().putLiteral("duration", 99000), runtime("duration", 99000));
        assertEquals("1 Tag 3 Stunden 30 Minuten", exportedDays.getFirst().stringValue());
    }

    @Test
    void preservesNullZeroAndTotalSeconds() throws Exception {
        var field = new DurationInputElement();
        assertNull(field.formatValue(null));
        assertEquals(new BigDecimal("9000"), field.formatValue(9000));
        assertEquals(new BigDecimal("9000"), field.formatValue("9000"));
        field.validate(null);
        field.validate(0);
        field.validate(97200);
        field.setRequired(true);
        assertThrows(ValidationException.class, () -> field.validate(null));
        field.validate(0);
    }

    @Test
    void rejectsInvalidValuesEvenWhenOptionalAndNeverRounds() {
        var field = new DurationInputElement().setPrecision(DurationPrecision.Second);
        assertEquals(new BigDecimal("1.5"), field.formatValue(1.5));
        for (Object value : List.of(-1, 1.5, "", "abc", Map.of(), Double.NaN, Double.POSITIVE_INFINITY,
                DurationValueUtils.MAX_SECONDS + 1)) {
            assertThrows(ValidationException.class, () -> field.validate(value), String.valueOf(value));
        }
    }

    @Test
    void enforcesPrecisionAndInclusiveBounds() throws Exception {
        var field = new DurationInputElement().setMinDuration(60L).setMaxDuration(120L);
        field.validate(60);
        field.validate(120);
        assertThrows(ValidationException.class, () -> field.validate(0));
        assertThrows(ValidationException.class, () -> field.validate(180));
        assertThrows(ValidationException.class, () -> field.validate(75));
        field.setPrecision(DurationPrecision.Second);
        field.validate(75);
    }

    @Test
    void formatsAndComparesTheNumericContract() {
        var field = new DurationInputElement();
        assertEquals("Keine Angabe", field.toDisplayValue(null));
        assertEquals("0 Minuten", field.toDisplayValue(BigDecimal.ZERO));
        assertEquals("27 Stunden 30 Minuten", field.toDisplayValue(BigDecimal.valueOf(99000)));
        field.setPrecision(DurationPrecision.Second);
        assertEquals("0 Sekunden", field.toDisplayValue(BigDecimal.ZERO));
        assertEquals("1 Stunde 1 Minute 1 Sekunde", field.toDisplayValue(BigDecimal.valueOf(3661)));
        assertTrue(field.evaluate(ConditionOperator.Equals, 60, "60.0"));
        assertTrue(field.evaluate(ConditionOperator.GreaterThan, 9000, 3600));
        assertTrue(field.evaluate(ConditionOperator.Empty, null, null));
        assertFalse(field.evaluate(ConditionOperator.Empty, 0, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void treatsMissingConditionValuesAsEmptyWithoutTreatingZeroAsEmpty(String value) {
        var field = new DurationInputElement();
        assertTrue(field.evaluate(ConditionOperator.Empty, value, "ignored"));
        assertFalse(field.evaluate(ConditionOperator.NotEmpty, value, "ignored"));
        assertTrue(field.evaluate(ConditionOperator.Equals, value, null));
        assertTrue(field.evaluate(ConditionOperator.Equals, null, value));
        assertFalse(field.evaluate(ConditionOperator.NotEquals, value, null));
        assertFalse(field.evaluate(ConditionOperator.GreaterThan, 60, value));
        assertFalse(field.evaluate(ConditionOperator.LessThan, value, 60));
        assertFalse(field.evaluate(ConditionOperator.Equals, 0, value));
        assertTrue(field.evaluate(ConditionOperator.NotEquals, 0, value));
        assertTrue(field.evaluate(ConditionOperator.NotEmpty, 0, value));
    }

    @ParameterizedTest
    @EnumSource(value = ConditionOperator.class, names = {
            "Equals", "NotEquals", "LessThan", "LessThanOrEqual", "GreaterThan", "GreaterThanOrEqual"
    })
    void rejectsMalformedConditionOperandsWithoutThrowingOrTreatingThemAsMissing(ConditionOperator operator) {
        var field = new DurationInputElement();
        for (Object invalid : List.of("abc", Map.of(), Double.NaN, Double.POSITIVE_INFINITY)) {
            assertFalse(field.evaluate(operator, 60, invalid));
            assertFalse(field.evaluate(operator, invalid, 60));
            assertFalse(field.evaluate(operator, null, invalid));
            assertFalse(field.evaluate(operator, invalid, null));
            assertFalse(field.evaluate(operator, invalid, invalid));
        }
    }

    @Test
    void keepsNumericComparisonThresholdsIndependentOfInputValidation() {
        var field = new DurationInputElement().setMinDuration(60L).setMaxDuration(120L);
        assertTrue(field.evaluate(ConditionOperator.GreaterThan, 60, -1));
        assertTrue(field.evaluate(ConditionOperator.GreaterThan, 60, 59.5));
        assertTrue(field.evaluate(ConditionOperator.LessThan, 60, 120.5));
        assertTrue(field.evaluate(ConditionOperator.LessThanOrEqual, 60, "60.0"));
        assertTrue(field.evaluate(ConditionOperator.GreaterThanOrEqual, 60, "60.0"));
        assertFalse(field.evaluate(ConditionOperator.NotEquals, 60, "60.0"));
        assertFalse(field.evaluate(ConditionOperator.Empty, "abc", null));
        assertFalse(field.evaluate(ConditionOperator.NotEmpty, "abc", null));
    }

    @ParameterizedTest
    @MethodSource("displayValues")
    void displaysRawValuesWithoutThrowing(Object value, String expected) {
        var field = new DurationInputElement();
        assertEquals(expected, field.display(value));
        if (value instanceof BigDecimal decimal) {
            assertEquals(expected, field.toDisplayValue(decimal));
        }
    }

    @ParameterizedTest
    @MethodSource("displayValues")
    void exportsAuthoredAndEffectiveValuesWithoutThrowing(Object value, String expected) {
        var field = new DurationInputElement();
        field.setId("duration");
        field.setTechnical(true);
        var formatter = new OZGCloudDataFormatService();
        var effectiveExport = formatter.buildFormData(field, new AuthoredElementValues(), runtime("duration", value));
        assertEquals(expected, effectiveExport.getFirst().stringValue());
        var authoredExport = formatter.buildFormData(
                field, new AuthoredElementValues().putLiteral("duration", value), runtime("duration", 60));
        assertEquals(expected, authoredExport.getFirst().stringValue());
    }

    private static Stream<Arguments> displayValues() {
        return Stream.of(
                Arguments.of(null, "Keine Angabe"),
                Arguments.of(0, "0 Minuten"),
                Arguments.of("60.0", "1 Minute"),
                Arguments.of(75, "1 Minute 15 Sekunden"),
                Arguments.of("", "Ungültige Zeitdauer"),
                Arguments.of("abc", "Ungültige Zeitdauer"),
                Arguments.of(Map.of(), "Ungültige Zeitdauer"),
                Arguments.of(Double.NaN, "Ungültige Zeitdauer"),
                Arguments.of(Double.POSITIVE_INFINITY, "Ungültige Zeitdauer"),
                Arguments.of(BigDecimal.valueOf(-1), "Ungültige Zeitdauer"),
                Arguments.of(BigDecimal.valueOf(1.5), "Ungültige Zeitdauer"),
                Arguments.of(BigDecimal.valueOf(DurationValueUtils.MAX_SECONDS + 1), "Ungültige Zeitdauer")
        );
    }

    @Test
    void registersAndRoundTripsTheElementIncludingSettings() throws Exception {
        assertInstanceOf(DurationInputElement.class, ElementType.getElementClass(ElementType.Duration));
        var mapper = JsonMapperFactory.getInstance();
        var field = (DurationInputElement) mapper.readValue("""
                {"type":55,"id":"duration","required":false,"precision":"second","largestUnit":"day","minDuration":1,"maxDuration":97200}
                """, BaseElement.class);
        assertEquals(DurationPrecision.Second, field.getPrecision());
        assertEquals(DurationLargestUnit.Day, field.getLargestUnit());
        assertEquals(1L, field.getMinDuration());
        assertEquals(97200L, field.getMaxDuration());
        var json = mapper.writeValueAsString(field);
        assertEquals(field, mapper.readValue(json, BaseElement.class));
        assertEquals("day", mapper.readTree(json).get("largestUnit").asText());
        var legacy = (DurationInputElement) mapper.readValue("{\"type\":55}", BaseElement.class);
        assertNull(legacy.getLargestUnit());
        assertEquals("27 Stunden", legacy.toDisplayValue(BigDecimal.valueOf(97200)));
    }
}
