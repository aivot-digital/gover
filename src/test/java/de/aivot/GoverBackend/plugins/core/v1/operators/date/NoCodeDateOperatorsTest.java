package de.aivot.GoverBackend.plugins.core.v1.operators.date;

import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static de.aivot.GoverBackend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeDateOperatorsTest {
    @Test
    void createTimeShouldCreateTimeAndValidateRange() throws NoCodeException {
        var operator = new NoCodeCreateTimeOperator();
        var data = runtime();

        var result = (ZonedDateTime) operator.evaluate(data, 13, 45).getValue();

        assertEquals(13, result.getHour());
        assertEquals(45, result.getMinute());
        assertEquals(0, result.getSecond());
        assertEquals(0, result.getNano());
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, 24, 0));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, 23, 60));
    }

    @Test
    void addToDateShouldAddRequestedUnitAndNormalizeUnitInput() throws NoCodeException {
        var operator = new NoCodeAddToDateOperator();
        var data = runtime();
        var baseDate = ZonedDateTime.of(2026, 3, 1, 12, 0, 0, 0, ZoneId.of("UTC"));

        var plusDays = (ZonedDateTime) operator.evaluate(data, baseDate, 5, "  TAGE  ").getValue();
        var plusWeeks = (ZonedDateTime) operator.evaluate(data, baseDate, 2, "Wochen").getValue();

        assertEquals(LocalDate.of(2026, 3, 6), plusDays.toLocalDate());
        assertEquals(LocalDate.of(2026, 3, 15), plusWeeks.toLocalDate());
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, baseDate, 1, "invalid"));
    }

    @Test
    void subtractFromDateShouldSubtractRequestedUnit() throws NoCodeException {
        var operator = new NoCodeSubtractFromDateOperator();
        var data = runtime();
        var baseDate = ZonedDateTime.of(2026, 3, 10, 12, 0, 0, 0, ZoneId.of("UTC"));

        var minusDays = (ZonedDateTime) operator.evaluate(data, baseDate, 5, "tage").getValue();
        var minusMonths = (ZonedDateTime) operator.evaluate(data, baseDate, 1, "monate").getValue();

        assertEquals(LocalDate.of(2026, 3, 5), minusDays.toLocalDate());
        assertEquals(LocalDate.of(2026, 2, 10), minusMonths.toLocalDate());
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, baseDate, 1, "invalid"));
    }

    @Test
    void createTodayShouldReturnTodayAtMidnight() throws NoCodeException {
        var operator = new NoCodeCreateTodayOperator();
        var data = runtime();

        var result = (ZonedDateTime) operator.evaluate(data).getValue();

        assertEquals(LocalDate.now(result.getZone()), result.toLocalDate());
        assertEquals(0, result.getHour());
        assertEquals(0, result.getMinute());
        assertEquals(0, result.getSecond());
        assertEquals(0, result.getNano());
    }

    @Test
    void createDateShouldCreateDateOrThrowOnInvalidInput() throws NoCodeException {
        var operator = new NoCodeCreateDateOperator();
        var data = runtime();

        var result = (ZonedDateTime) operator.evaluate(data, 15, 8, 2026).getValue();

        assertEquals(LocalDate.of(2026, 8, 15), result.toLocalDate());
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, 31, 2, 2026));
        assertTrue(result.getHour() == 0 && result.getMinute() == 0);
    }
}
