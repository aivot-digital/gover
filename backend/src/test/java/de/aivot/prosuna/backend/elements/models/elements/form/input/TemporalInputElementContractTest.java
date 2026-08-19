package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.DateType;
import de.aivot.prosuna.backend.enums.TimeType;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalInputElementContractTest {
    private ZoneId originalZoneId;

    @BeforeEach
    void configureApplicationTimeZone() {
        originalZoneId = ApplicationTimeZone.getZoneId();
        ApplicationTimeZone.configure(ZoneId.of("Europe/Berlin"));
    }

    @AfterEach
    void restoreApplicationTimeZone() {
        ApplicationTimeZone.configure(originalZoneId);
    }

    @Test
    void dateInput_AcceptsCanonicalLocalDatePrecisionsAndRejectsInstants() {
        assertEquals(LocalDate.of(2026, 7, 29), DateInputElement._formatValue("2026-07-29"));
        assertEquals(YearMonth.of(2026, 7), DateInputElement._formatValue("2026-07"));
        assertEquals(Year.of(2026), DateInputElement._formatValue("2026"));
        assertNull(DateInputElement._formatValue("2026-07-29T00:00:00Z"));
        assertNull(DateInputElement._formatValue("29.07.2026"));
    }

    @Test
    void dateInput_AcceptsLegacyAndPartialFormatsOnlyAsAuthoredConditionValues() {
        var element = new DateInputElement();

        assertTrue(element.evaluate(ConditionOperator.Equals, "2026-07-29", "29.07.2026"));
        assertTrue(element.evaluate(ConditionOperator.Equals, "2026-01-31", "31."));
        assertTrue(element.evaluate(ConditionOperator.Equals, "2028-02-29", "29.02."));
        assertFalse(element.evaluate(ConditionOperator.Equals, "29.07.2026", "29.07.2026"));
    }

    @Test
    void dateInput_UsesAndSerializesTheConfiguredSemanticType() {
        assertEquals(
                "2026-07-29",
                toJsonValue(new DateInputElement().formatValue("2026-07-29"))
        );
        assertEquals(
                "2026-07",
                toJsonValue(new DateInputElement()
                        .setMode(DateType.Month)
                        .formatValue("2026-07-29"))
        );
        assertEquals(
                "2026",
                toJsonValue(new DateInputElement()
                        .setMode(DateType.Year)
                        .formatValue("2026-07-29"))
        );
    }

    @Test
    void timeInput_AcceptsLocalTimesAndInternalInstantsButRejectsInstantStrings() {
        var element = new TimeInputElement();

        assertEquals(LocalTime.of(9, 30), element.formatValue("09:30"));
        assertEquals(LocalTime.of(9, 30), element.formatValue("09:30:15"));
        assertEquals(
                LocalTime.of(9, 30, 15),
                new TimeInputElement().setMode(TimeType.Second).formatValue("09:30:15")
        );
        assertEquals(
                LocalTime.of(9, 30, 15),
                new TimeInputElement()
                        .setMode(TimeType.Second)
                        .formatValue(Instant.parse("2026-07-29T07:30:15Z"))
        );
        assertNull(element.formatValue("2026-07-29T09:30:00+02:00"));
    }

    @Test
    void timeInput_ComparesSecondsWithoutHourOrMinuteShortcuts() {
        var element = new TimeInputElement().setMode(TimeType.Second);

        assertFalse(element.evaluate(ConditionOperator.LessThanOrEqual, "10:59:59", "10:00:00"));
        assertTrue(element.evaluate(ConditionOperator.LessThan, "09:30:14", "09:30:15"));
    }

    @Test
    void timeInput_SerializesLocalTimeWithStableSecondPrecision() {
        assertEquals("09:30:00", toJsonValue(new TimeInputElement().formatValue("09:30:15")));
        assertEquals("09:30:15", toJsonValue(
                new TimeInputElement().setMode(TimeType.Second).formatValue("09:30:15")
        ));
    }

    @Test
    void dateTimeInput_RequiresAnExplicitInstant() {
        assertEquals(
                Instant.parse("2026-07-29T07:00:00Z"),
                DateTimeInputElement._formatValue("2026-07-29T09:00:00+02:00")
        );
        assertEquals(
                Instant.parse("2026-07-29T07:00:00Z"),
                DateTimeInputElement._formatValue("2026-07-29T09:00:00")
        );
        assertNull(DateTimeInputElement._formatValue("29.07.2026 09:00"));
    }

    @Test
    void dateTimeInput_SerializesPayloadWithApplicationOffset() {
        assertEquals(
                "2026-07-29T09:00:00+02:00",
                toJsonValue(new DateTimeInputElement().formatValue("2026-07-29T07:00:00Z"))
        );
    }

    @Test
    void dateTimeInput_ComparesInstantsAtNanosecondPrecision() {
        var element = new DateTimeInputElement();

        assertTrue(element.evaluate(
                ConditionOperator.LessThan,
                "2026-07-29T07:00:00.000000001Z",
                "2026-07-29T07:00:00.000000002Z"
        ));
        assertFalse(element.evaluate(
                ConditionOperator.Equals,
                "2026-07-29T07:00:00.000000001Z",
                "2026-07-29T07:00:00.000000002Z"
        ));
    }

    @Test
    void temporalRanges_SerializeEachEndpointAccordingToTheirSemanticType() {
        assertEquals(
                Map.of("start", "2026-07", "end", "2026-08"),
                toJsonValue(new DateRangeInputElement()
                        .setMode(DateType.Month)
                        .formatValue(Map.of("start", "2026-07", "end", "2026-08")))
        );
        assertEquals(
                Map.of("start", "09:30:15", "end", "10:45:30"),
                toJsonValue(new TimeRangeInputElement()
                        .setMode(TimeType.Second)
                        .formatValue(Map.of("start", "09:30:15", "end", "10:45:30")))
        );
        assertEquals(
                Map.of(
                        "start", "2026-07-29T09:00:00+02:00",
                        "end", "2026-07-29T10:00:00+02:00"
                ),
                toJsonValue(new DateTimeRangeInputElement().formatValue(Map.of(
                        "start", "2026-07-29T07:00:00Z",
                        "end", "2026-07-29T08:00:00Z"
                )))
        );
    }

    private static Object toJsonValue(Object value) {
        return ObjectMapperFactory.getInstance().convertValue(value, Object.class);
    }
}
