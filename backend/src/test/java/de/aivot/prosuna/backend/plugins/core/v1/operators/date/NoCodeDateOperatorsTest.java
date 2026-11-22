package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DateInputElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeLessThanOperator;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeDateOperatorsTest {
    private static final ZoneId TEST_ZONE = ZoneId.of("Europe/Berlin");
    private static final Instant FIXED_NOW = Instant.parse("2026-07-29T07:15:30Z");

    private ZoneId originalZone;
    private BusinessTime businessTime;

    @BeforeEach
    void setUp() {
        originalZone = ApplicationTimeZone.getZoneId();
        ApplicationTimeZone.configure(TEST_ZONE);
        businessTime = new BusinessTime(TEST_ZONE, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        ApplicationTimeZone.configure(originalZone);
    }

    @Test
    void createTimeShouldCreateTimeAndValidateRange() throws NoCodeException {
        var operator = new NoCodeCreateTimeOperator();
        var data = runtime();

        var result = (LocalTime) operator.evaluate(data, 13, 45).getValue();
        var resultWithSeconds = (LocalTime) operator.evaluate(data, 13, 45, 27).getValue();

        assertEquals(13, result.getHour());
        assertEquals(45, result.getMinute());
        assertEquals(0, result.getSecond());
        assertEquals(0, result.getNano());
        assertEquals(LocalTime.of(13, 45, 27), resultWithSeconds);
        assertNull(operator.getHumanReadableTemplate());
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, 24, 0));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, 23, 60));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, 23, 59, 60));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, "Stunde", 30));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, 13.5, 30));
    }

    @Test
    void commonProviderShouldRegisterTheTemporalOperators() {
        var identifiers = Arrays.stream(new CommonOperatorsV1(null, null, businessTime).getOperators())
                .map(operator -> operator.getIdentifier())
                .collect(Collectors.toSet());

        assertTrue(identifiers.containsAll(Set.of(
                "create-now",
                "combine-date-and-time",
                "extract-date-from-datetime",
                "extract-time-from-datetime",
                "format-datetime",
                "format-time",
                "compare-date",
                "compare-time",
                "compare-datetime",
                "add-to-datetime",
                "subtract-from-datetime"
        )));
    }

    @Test
    void addToDateShouldAddRequestedUnitAndNormalizeUnitInput() throws NoCodeException {
        var operator = new NoCodeAddToDateOperator();
        var data = runtime();
        var baseDate = LocalDate.of(2026, 3, 1);

        var plusDays = (LocalDate) operator.evaluate(data, baseDate, 5, "  TAGE  ").getValue();
        var plusWeeks = (LocalDate) operator.evaluate(data, baseDate, 2, "Wochen").getValue();

        assertEquals(LocalDate.of(2026, 3, 6), plusDays);
        assertEquals(LocalDate.of(2026, 3, 15), plusWeeks);
        assertEquals(
                LocalDate.of(2026, 2, 28),
                operator.evaluate(data, LocalDate.of(2026, 1, 31), 1, "monate").getValue()
        );
        assertEquals(
                YearMonth.of(2026, 4),
                operator.evaluate(data, YearMonth.of(2026, 3), 1, "monate").getValue()
        );
        assertEquals(
                Year.of(2027),
                operator.evaluate(data, Year.of(2026), 1, "jahre").getValue()
        );
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, baseDate, 1, "invalid"));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, "invalid-date", 1, "tage"));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, "2026-03", 1, "tage"));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, baseDate, 1.5, "tage"));
    }

    @Test
    void subtractFromDateShouldSubtractRequestedUnit() throws NoCodeException {
        var operator = new NoCodeSubtractFromDateOperator();
        var data = runtime();
        var baseDate = LocalDate.of(2026, 3, 10);

        var minusDays = (LocalDate) operator.evaluate(data, baseDate, 5, "tage").getValue();
        var minusMonths = (LocalDate) operator.evaluate(data, baseDate, 1, "monate").getValue();

        assertEquals(LocalDate.of(2026, 3, 5), minusDays);
        assertEquals(LocalDate.of(2026, 2, 10), minusMonths);
        assertEquals(
                YearMonth.of(2026, 2),
                operator.evaluate(data, YearMonth.of(2026, 3), 1, "monate").getValue()
        );
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, baseDate, 1, "invalid"));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, "invalid-date", 1, "tage"));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, Year.of(2026), 1, "monate"));
    }

    @Test
    void createTodayShouldReturnApplicationLocalDate() throws NoCodeException {
        var operator = new NoCodeCreateTodayOperator(businessTime);
        var data = runtime();

        var result = (LocalDate) operator.evaluate(data).getValue();

        assertEquals(LocalDate.of(2026, 7, 29), result);
    }

    @Test
    void createNowShouldReturnAbsoluteBusinessClockInstant() throws NoCodeException {
        var operator = new NoCodeCreateNowOperator(businessTime);

        assertEquals(FIXED_NOW, operator.evaluate(runtime()).getValue());
    }

    @Test
    void combineDateAndTimeShouldResolveApplicationTimeZoneAndEnforceDstPolicy() throws NoCodeException {
        var operator = new NoCodeCombineDateAndTimeOperator(businessTime);
        var data = runtime();

        assertEquals(
                Instant.parse("2026-07-29T07:15:30Z"),
                operator.evaluate(data, "2026-07-29", "09:15:30").getValue()
        );
        assertEquals(
                // During the overlap, BusinessTime chooses the earlier execution (+02:00).
                Instant.parse("2026-10-25T00:30:00Z"),
                operator.evaluate(data, "2026-10-25", "02:30:00").getValue()
        );
        assertThrows(
                NoCodeException.class,
                () -> operator.evaluate(data, "2026-03-29", "02:30:00")
        );
        assertThrows(
                NoCodeException.class,
                () -> operator.evaluate(data, "2026-07", "09:15:30")
        );
    }

    @Test
    void extractDateTimePartsShouldUseApplicationTimeZone() throws NoCodeException {
        var dateOperator = new NoCodeExtractDateTimePartOperator(
                NoCodeExtractDateTimePartOperator.Part.DATE,
                businessTime
        );
        var timeOperator = new NoCodeExtractDateTimePartOperator(
                NoCodeExtractDateTimePartOperator.Part.TIME,
                businessTime
        );
        var instant = "2026-07-29T23:15:30+00:00";

        assertEquals(LocalDate.of(2026, 7, 30), dateOperator.evaluate(runtime(), instant).getValue());
        assertEquals(LocalTime.of(1, 15, 30), timeOperator.evaluate(runtime(), instant).getValue());
    }

    @Test
    void temporalComparisonShouldRespectEachTypesSemantics() throws NoCodeException {
        var dateOperator = new NoCodeTemporalCompareOperator(NoCodeTemporalCompareOperator.TemporalType.DATE);
        var timeOperator = new NoCodeTemporalCompareOperator(NoCodeTemporalCompareOperator.TemporalType.TIME);
        var dateTimeOperator = new NoCodeTemporalCompareOperator(NoCodeTemporalCompareOperator.TemporalType.DATETIME);
        var data = runtime();

        assertTrue((Boolean) dateOperator.evaluate(data, "2026-07-29", "vor", "2026-07-30").getValue());
        assertTrue((Boolean) dateOperator.evaluate(data, "2026-07", "vor", "2026-08").getValue());
        assertTrue((Boolean) dateOperator.evaluate(data, "2026", "gleich", Year.of(2026)).getValue());
        assertTrue((Boolean) timeOperator.evaluate(data, "09:15:30", "nach", "08:15:30").getValue());
        assertTrue((Boolean) dateTimeOperator.evaluate(
                data,
                "2026-07-29T09:15:30+02:00",
                "gleich",
                "2026-07-29T07:15:30+00:00"
        ).getValue());
        assertFalse((Boolean) dateTimeOperator.evaluate(
                data,
                "2026-07-29T09:15:30+02:00",
                "vor",
                "2026-07-29T07:15:30+00:00"
        ).getValue());
        assertThrows(
                NoCodeException.class,
                () -> dateOperator.evaluate(data, "2026-07-29", "irgendwie", "2026-07-30")
        );
        assertThrows(
                NoCodeException.class,
                () -> dateOperator.evaluate(data, "2026-07", "gleich", "2026-07-01")
        );
    }

    @Test
    void createDateShouldCreateDateOrThrowOnInvalidInput() throws NoCodeException {
        var operator = new NoCodeCreateDateOperator();
        var data = runtime();

        var result = (LocalDate) operator.evaluate(data, 15, 8, 2026).getValue();

        assertEquals(LocalDate.of(2026, 8, 15), result);
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, 31, 2, 2026));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, "Tag", 8, 2026));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, 15.5, 8, 2026));
    }

    @Test
    void formatDateShouldThrowNoCodeExceptionOnInvalidDateInput() {
        var operator = new NoCodeFormatDateOperator(businessTime);
        var data = runtime();

        assertThrows(NoCodeException.class, () -> operator.evaluate(data, "invalid-date", "dd.MM.yyyy"));
    }

    @Test
    void formatDateShouldSupportStaticPartialDatesAndRejectTimeFieldsForCalendarDates() throws NoCodeException {
        var operator = new NoCodeFormatDateOperator(businessTime);
        var data = runtime();

        assertEquals("07.2026", operator.evaluate(data, "2026-07", "MM.yyyy").getValue());
        assertEquals("2026", operator.evaluate(data, "2026", "yyyy").getValue());
        assertThrows(
                NoCodeException.class,
                () -> operator.evaluate(data, "2026-07", "dd.MM.yyyy")
        );
        assertThrows(
                NoCodeException.class,
                () -> operator.evaluate(data, LocalDate.of(2026, 7, 29), "dd.MM.yyyy HH:mm")
        );
    }

    @Test
    void formatDateTimeShouldUseApplicationTimeZoneAndHaveAnUnambiguousSignature() throws NoCodeException {
        var dateOperator = new NoCodeFormatDateOperator(businessTime);
        var dateTimeOperator = new NoCodeFormatDateTimeOperator(businessTime);
        var data = runtime();

        assertEquals(1, dateOperator.getSignatures().length);
        assertEquals(1, dateTimeOperator.getSignatures().length);
        assertEquals(
                NoCodeDataType.Date,
                dateOperator.getSignatures()[0].parameters()[0].type()
        );
        assertEquals(
                NoCodeDataType.DateTime,
                dateTimeOperator.getSignatures()[0].parameters()[0].type()
        );
        assertEquals(
                "29.07.2026 09:15",
                dateTimeOperator.evaluate(
                        data,
                        "2026-07-29T07:15:30+00:00",
                        "dd.MM.yyyy HH:mm"
                ).getValue()
        );
        assertEquals(
                "2026-07-29T09:15:30+02:00",
                dateTimeOperator.evaluate(
                        data,
                        "2026-07-29T07:15:30+00:00",
                        "yyyy-MM-dd'T'HH:mm:ssXXX"
                ).getValue()
        );
        assertThrows(
                NoCodeException.class,
                () -> dateTimeOperator.evaluate(data, "2026-07-29", "dd.MM.yyyy")
        );

        // Stored format-date expressions accepted DateTime values before the dedicated
        // operator existed and must continue to evaluate successfully.
        assertEquals(
                "29.07.2026 09:15",
                dateOperator.evaluate(
                        data,
                        "2026-07-29T07:15:30+00:00",
                        "dd.MM.yyyy HH:mm"
                ).getValue()
        );
    }

    @Test
    void formatTimeShouldSupportCanonicalDisplayPrecisions() throws NoCodeException {
        var operator = new NoCodeFormatTimeOperator();
        var data = runtime();

        assertEquals("09:15", operator.evaluate(data, "09:15:30", "HH:mm").getValue());
        assertEquals("09:15:30", operator.evaluate(data, "09:15:30", "HH:mm:ss").getValue());
        assertThrows(
                NoCodeException.class,
                () -> operator.evaluate(data, "09:15:30", "hh:mm a")
        );
    }

    @Test
    void addToDateTimeShouldKeepLocalTimeInApplicationZone() throws NoCodeException {
        var operator = new NoCodeAdjustDateTimeOperator(NoCodeAdjustDateTimeOperator.Direction.ADD);
        var data = runtime();

        // 12:00 in Berlin before and after the switch to daylight saving time on 2026-03-29.
        assertEquals(
                Instant.parse("2026-03-29T10:00:00Z"),
                operator.evaluate(data, Instant.parse("2026-03-28T11:00:00Z"), 1, "tage").getValue()
        );
        assertEquals(
                Instant.parse("2026-02-28T10:00:00Z"),
                operator.evaluate(data, "2026-01-31T10:00:00Z", 1, " Monate ").getValue()
        );
        assertEquals(
                Instant.parse("2026-08-12T07:15:30Z"),
                operator.evaluate(data, FIXED_NOW, 2, "wochen").getValue()
        );
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, FIXED_NOW, 1, "stunden"));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, "2026-07-29", 1, "tage"));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, FIXED_NOW, 1.5, "tage"));
    }

    /**
     * Legacy YearsInPast on a date-time field compares against now minus N years with second precision.
     */
    @Test
    void subtractFromDateTimeShouldReplaceLegacyRelativeDateTimeConditions() throws NoCodeException {
        var subtract = new NoCodeAdjustDateTimeOperator(NoCodeAdjustDateTimeOperator.Direction.SUBTRACT);
        var compare = new NoCodeTemporalCompareOperator(NoCodeTemporalCompareOperator.TemporalType.DATETIME);
        var data = runtime();

        var target = subtract.evaluate(data, new NoCodeCreateNowOperator(businessTime).evaluate(data).getValue(), 18, "jahre").getValue();

        assertEquals(Instant.parse("2008-07-29T07:15:30Z"), target);
        assertTrue(compare.evaluate(data, "2008-07-29T07:15:30Z", "vor-oder-gleich", target).getValueAsBoolean());
        assertFalse(compare.evaluate(data, "2008-07-29T07:15:31Z", "vor-oder-gleich", target).getValueAsBoolean());
    }

    /**
     * Legacy date conditions derive the precision from the compared text (MM.yyyy, yyyy, dd.MM.). The no-code
     * recipe formats the field to the same precision first.
     */
    @Test
    void formatDateRecipesShouldReplaceLegacyPartialDateConditions() throws NoCodeException {
        var element = new DateInputElement();
        var format = new NoCodeFormatDateOperator(businessTime);
        var compare = new NoCodeTemporalCompareOperator(NoCodeTemporalCompareOperator.TemporalType.DATE);
        var lessThan = new NoCodeLessThanOperator();
        var data = runtime();

        for (var value : List.of("2026-02-28", "2026-03-15", "2026-04-01", "2025-12-31")) {
            var month = format.evaluate(data, value, "yyyy-MM").getValue();
            assertEquals(
                    element.evaluate(ConditionOperator.LessThan, value, "03.2026"),
                    compare.evaluate(data, month, "vor", "2026-03").getValueAsBoolean(),
                    "month " + value
            );

            var year = format.evaluate(data, value, "yyyy").getValue();
            assertEquals(
                    element.evaluate(ConditionOperator.Equals, value, "2026"),
                    compare.evaluate(data, year, "gleich", "2026").getValueAsBoolean(),
                    "year " + value
            );

            var dayAndMonth = format.evaluate(data, value, "MMdd").getValue();
            assertEquals(
                    element.evaluate(ConditionOperator.LessThan, value, "15.03."),
                    lessThan.evaluate(data, dayAndMonth, 315).getValueAsBoolean(),
                    "any year " + value
            );
        }
    }
}
