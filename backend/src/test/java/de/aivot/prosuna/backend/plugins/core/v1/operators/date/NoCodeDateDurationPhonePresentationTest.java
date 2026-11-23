package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeOperatorDetailsDTO;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDateInput;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.prosuna.backend.plugins.core.v1.operators.duration.NoCodeConvertDurationOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.duration.NoCodeCreateDurationOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.duration.NoCodeFormatDurationOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.phone.NoCodePhoneNumberFormatOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.phone.NoCodePhoneNumberIsPossibleOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.phone.NoCodePhoneNumberIsValidOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.phone.NoCodePhoneNumberNormalizeOperator;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeDateDurationPhonePresentationTest {
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final Instant EXAMPLE = Instant.parse("2026-11-22T08:30:00Z");
    private final BusinessTime businessTime = new BusinessTime(ZONE, Clock.fixed(EXAMPLE, ZoneOffset.UTC));
    private ZoneId originalZone;

    @BeforeEach
    void configureApplicationZone() {
        originalZone = ApplicationTimeZone.getZoneId();
        ApplicationTimeZone.configure(ZONE);
    }

    @AfterEach
    void restoreApplicationZone() {
        ApplicationTimeZone.configure(originalZone);
    }

    @Test
    void describedDateExamplesShouldComposeAsActualEditorExpressions() throws Exception {
        var date = new NoCodeCreateDateOperator().evaluate(runtime(), 22, 11, 2026).getValue();
        var time = new NoCodeCreateTimeOperator().evaluate(runtime(), 9, 30, 0).getValue();
        var combined = new NoCodeCombineDateAndTimeOperator(businessTime).evaluate(runtime(), date, time).getValue();

        assertEquals(LocalDate.of(2026, 11, 22), date);
        assertEquals(LocalTime.of(9, 30), time);
        assertEquals(EXAMPLE, combined);
        assertEquals(date, new NoCodeExtractDateTimePartOperator(
                NoCodeExtractDateTimePartOperator.Part.DATE, businessTime
        ).evaluate(runtime(), combined).getValue());
        assertEquals(time, new NoCodeExtractDateTimePartOperator(
                NoCodeExtractDateTimePartOperator.Part.TIME, businessTime
        ).evaluate(runtime(), combined).getValue());
        assertEquals("22.11.2026", new NoCodeFormatDateOperator(businessTime)
                .evaluate(runtime(), date, "dd.MM.yyyy").getValue());
        assertEquals("22.11.2026 09:30", new NoCodeFormatDateTimeOperator(businessTime)
                .evaluate(runtime(), combined, "dd.MM.yyyy HH:mm").getValue());
        assertEquals("09:30", new NoCodeFormatTimeOperator()
                .evaluate(runtime(), "09:30:45", "HH:mm").getValue());
    }

    @Test
    void extractedTimeShouldDropFractionsWithoutRoundingAndRespectApplicationDay() throws Exception {
        var dateOperator = new NoCodeExtractDateTimePartOperator(NoCodeExtractDateTimePartOperator.Part.DATE, businessTime);
        var timeOperator = new NoCodeExtractDateTimePartOperator(NoCodeExtractDateTimePartOperator.Part.TIME, businessTime);
        var source = Instant.parse("2026-11-22T23:59:59.999999999Z");

        assertEquals(LocalDate.of(2026, 11, 23), dateOperator.evaluate(runtime(), source).getValue());
        assertEquals(LocalTime.of(0, 59, 59), timeOperator.evaluate(runtime(), source).getValue());
        assertEquals("00:59", new NoCodeFormatTimeOperator().evaluate(
                runtime(), timeOperator.evaluate(runtime(), source).getValue(), "HH:mm"
        ).getValue());
    }

    @Test
    void durationExamplesShouldDistinguishSecondsTotalsAndFormattedParts() throws Exception {
        var seconds = new NoCodeCreateDurationOperator().evaluate(runtime(), "1.5", "hours").getValue();

        assertEquals(0, new BigDecimal("5400").compareTo((BigDecimal) seconds));
        assertEquals(0, new BigDecimal("2.5").compareTo((BigDecimal) new NoCodeConvertDurationOperator()
                .evaluate(runtime(), 9000, "hours").getValue()));
        assertEquals("1 Tag 3 Stunden 30 Minuten", new NoCodeFormatDurationOperator()
                .evaluate(runtime(), 99000, "days").getValue());
        assertEquals("1 Stunde 30 Minuten", new NoCodeFormatDurationOperator()
                .evaluate(runtime(), seconds, "hours").getValue());
    }

    @Test
    void durationUnitGrammarShouldUseTheAuthoredQuantityOnlyForCreation() {
        var createUnit = new NoCodeCreateDurationOperator().getSignatures()[0].parameters()[1];
        var convertUnit = new NoCodeConvertDurationOperator().getSignatures()[0].parameters()[1];

        assertEquals(0, createUnit.quantityParameterIndex());
        assertNull(convertUnit.quantityParameterIndex());
        assertTrue(convertUnit.optionsAsSentencePart());
        assertEquals("hours", createUnit.options()[2].value());
        assertEquals("Stunden", createUnit.options()[2].label());
        assertEquals("Stunde", createUnit.options()[2].singularLabel());
        assertTrue(new NoCodeFormatDurationOperator().getHumanReadableTemplate().contains("#1"));
        assertTrue(new NoCodeFormatDurationOperator().getSignatures()[0].parameters()[1].optionsAsSentencePart());
    }

    @Test
    void phoneExamplesShouldDistinguishPlausibilityValidationAndPresentation() throws Exception {
        var possible = new NoCodePhoneNumberIsPossibleOperator();
        var valid = new NoCodePhoneNumberIsValidOperator();
        var normalize = new NoCodePhoneNumberNormalizeOperator();
        var format = new NoCodePhoneNumberFormatOperator();

        assertEquals(true, possible.evaluate(runtime(), "+49 30 123456").getValue());
        assertEquals(true, valid.evaluate(runtime(), "+49 30 123456").getValue());
        assertEquals(true, possible.evaluate(runtime(), "+49 1234").getValue());
        assertEquals(false, valid.evaluate(runtime(), "+49 1234").getValue());
        var normalized = normalize.evaluate(runtime(), "+49 30 123456").getValue();
        assertEquals("+4930123456", normalized);
        assertEquals("+49 30 123456", format.evaluate(runtime(), normalized).getValue());
        assertEquals("030 123456", format.evaluate(runtime(), normalized, "national").getValue());
        assertNull(normalize.evaluate(runtime(), "030 123456").getValue());
        assertNull(format.evaluate(runtime(), "+49 30 123456 ext. 7").getValue());
    }

    @Test
    void publishesAllFifteenReviewedVariantsWithoutChangingChoiceValues() {
        var identifiers = Set.of("combine-date-and-time", "create-date", "create-time",
                "extract-date-from-datetime", "extract-time-from-datetime", "format-date", "format-datetime", "format-time",
                "create-duration", "convert-duration", "format-duration", "phone-number-is-possible", "phone-number-is-valid",
                "phone-number-normalize", "phone-number-format");
        var operators = Arrays.stream(new CommonOperatorsV1(null, null, businessTime).getOperators())
                .filter(operator -> identifiers.contains(operator.getIdentifier())).toList();
        assertEquals(15, operators.size());

        var formatParameters = operators.stream().filter(operator -> Set.of("format-date", "format-datetime", "format-time")
                .contains(operator.getIdentifier())).map(operator -> operator.getSignatures()[0].parameters()[1]).toList();
        for (var parameter : formatParameters) assertEquals("Ausgabeformat", parameter.label());

        var details = NoCodeOperatorDetailsDTO.fromSPI(new CommonOperatorsV1(null, null, businessTime))
                .filter(detail -> identifiers.contains(detail.identifier())).collect(Collectors.toMap(NoCodeOperatorDetailsDTO::identifier, detail -> detail));
        assertEquals(NoCodeDateInput.CalendarOrDateTime, details.get("format-date").signatures()[0].parameters()[0].dateInput());
        assertEquals("yyyy-MM-dd'T'HH:mm:ssXXX", details.get("format-datetime").signatures()[0].parameters()[1].options()[2].value());
        assertEquals("HH:mm:ss", details.get("format-time").signatures()[0].parameters()[1].options()[1].value());
        assertEquals("11", details.get("create-date").signatures()[0].parameters()[1].options()[10].value());
    }

    @Test
    void checksDurationInputForWholeSecondsWithoutRounding() {
        assertThrows(NoCodeException.class, () -> new NoCodeConvertDurationOperator()
                .evaluate(runtime(), new BigDecimal("0.999999999"), "seconds"));
    }
}
