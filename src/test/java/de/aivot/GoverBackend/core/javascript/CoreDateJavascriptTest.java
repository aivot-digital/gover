package de.aivot.GoverBackend.core.javascript;

import de.aivot.GoverBackend.plugins.corePlugin.components.javascript.DateJavascript;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.*;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class CoreDateJavascriptTest {
    private DateJavascript provider;
    private static final ZoneId TEST_ZONE = ZoneId.of("Europe/Berlin");
    private ZoneId originalZone;

    @BeforeAll
    static void beforeAll() {
        TimeZone.setDefault(TimeZone.getTimeZone(TEST_ZONE));
    }

    @BeforeEach
    void setUp() {
        provider = new DateJavascript();
        originalZone = ZoneId.systemDefault();
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(TimeZone.getTimeZone(originalZone));
    }

    @Test
    void testGetPackageName() {
        assertEquals("_date", provider.getPackageName());
    }

    @Test
    void testGetLabel() {
        assertEquals("Datumsangaben", provider.getLabel());
    }

    @Test
    void testGetDescription() {
        assertEquals("Dieses Paket enthält Funktionen für Datumsoperationen.", provider.getDescription());
    }

    @Test
    void testCreateDateNoArg() {
        ZonedDateTime now = ZonedDateTime.now(TEST_ZONE).toLocalDate().atStartOfDay(TEST_ZONE);
        ZonedDateTime result = provider.createDate();
        assertEquals(now.toLocalDate(), result.toLocalDate());
        assertEquals(TEST_ZONE, result.getZone());
    }

    @Test
    void testCreateDateNull() {
        assertNull(provider.createDate(null));
    }

    @Test
    void testCreateDateZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2020, 2, 29, 12, 0, 0, 0, TEST_ZONE);
        assertEquals(zdt, provider.createDate(zdt));
    }

    @Test
    void testCreateDateNumber() {
        long epochSecond = 1609459200L; // 2021-01-01T00:00:00Z
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), TEST_ZONE);
        assertEquals(expected, provider.createDate(epochSecond));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2021-01-01", "01.01.2021", "2021-01-01T00:00:00Z"})
    void testCreateDateStringValid(String dateStr) {
        ZonedDateTime result = provider.createDate(dateStr);
        assertNotNull(result);
        assertEquals(2021, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void testCreateDateStringInvalid() {
        assertNull(provider.createDate("not-a-date"));
    }

    @Test
    void testIsSameDay() {
        ZonedDateTime d1 = ZonedDateTime.of(2021, 3, 28, 0, 0, 0, 0, TEST_ZONE); // DST start in Berlin
        ZonedDateTime d2 = ZonedDateTime.of(2021, 3, 28, 23, 59, 59, 0, TEST_ZONE);
        assertTrue(provider.isSameDay(d1, d2));
        assertFalse(provider.isSameDay(d1, d1.plusDays(1)));
        assertFalse(provider.isSameDay(null, d2));
        assertFalse(provider.isSameDay(d1, null));
    }

    @Test
    void testIsBefore() {
        ZonedDateTime d1 = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, TEST_ZONE);
        ZonedDateTime d2 = ZonedDateTime.of(2021, 1, 2, 0, 0, 0, 0, TEST_ZONE);
        assertTrue(provider.isBefore(d1, d2));
        assertFalse(provider.isBefore(d2, d1));
        assertFalse(provider.isBefore(d1, d1));
        assertFalse(provider.isBefore(null, d2));
        assertFalse(provider.isBefore(d1, null));
    }

    @Test
    void testIsBeforeOrSameDay() {
        ZonedDateTime d1 = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, TEST_ZONE);
        ZonedDateTime d2 = ZonedDateTime.of(2021, 1, 2, 0, 0, 0, 0, TEST_ZONE);
        assertTrue(provider.isBeforeOrSameDay(d1, d2));
        assertTrue(provider.isBeforeOrSameDay(d1, d1));
        assertFalse(provider.isBeforeOrSameDay(d2, d1));
    }

    @Test
    void testIsAfter() {
        ZonedDateTime d1 = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, TEST_ZONE);
        ZonedDateTime d2 = ZonedDateTime.of(2021, 1, 2, 0, 0, 0, 0, TEST_ZONE);
        assertTrue(provider.isAfter(d2, d1));
        assertFalse(provider.isAfter(d1, d2));
        assertFalse(provider.isAfter(d1, d1));
        assertFalse(provider.isAfter(null, d2));
        assertFalse(provider.isAfter(d1, null));
    }

    @Test
    void testIsAfterOrSameDay() {
        ZonedDateTime d1 = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, TEST_ZONE);
        ZonedDateTime d2 = ZonedDateTime.of(2021, 1, 2, 0, 0, 0, 0, TEST_ZONE);
        assertTrue(provider.isAfterOrSameDay(d2, d1));
        assertTrue(provider.isAfterOrSameDay(d1, d1));
        assertFalse(provider.isAfterOrSameDay(d1, d2));
    }

    @Test
    void testAddSubtractDaysWeeksMonthsYears() {
        ZonedDateTime base = ZonedDateTime.of(2020, 2, 29, 0, 0, 0, 0, TEST_ZONE); // Leap year
        assertEquals(base.plusDays(1), provider.addDays(base, 1));
        assertEquals(base.minusDays(1), provider.subtractDays(base, 1));
        assertEquals(base.plusWeeks(1), provider.addWeeks(base, 1));
        assertEquals(base.minusWeeks(1), provider.subtractWeeks(base, 1));
        assertEquals(base.plusMonths(1), provider.addMonths(base, 1));
        assertEquals(base.minusMonths(1), provider.subtractMonths(base, 1));
        assertEquals(base.plusYears(1), provider.addYears(base, 1));
        assertEquals(base.minusYears(1), provider.subtractYears(base, 1));
        assertNull(provider.addDays(null, 1));
        assertNull(provider.subtractDays(null, 1));
    }

    @Test
    void testDiffBasicFunctionality() {
        assertEquals(1, provider.diff("2021-01-01", "2021-01-02", "days"));
        assertEquals(1.0f, provider.diff("2021-01-01", "2021-01-08", "weeks"));
        assertEquals(1, provider.diff("2021-01-01", "2021-02-01", "months"));
        assertEquals(1, provider.diff("2020-01-01", "2021-01-01", "years"));
        assertEquals(-1, provider.diff("2021-01-02", "2021-01-01", "days"));
    }

    @Test
    void testDiffDSTTransitions() {
        // DST starts in Berlin: 2021-03-28
        assertEquals(1, provider.diff("2021-03-27", "2021-03-28", "days"));
        // DST ends in Berlin: 2021-10-31
        assertEquals(1, provider.diff("2021-10-30", "2021-10-31", "days"));
        // Same calendar day, different offsets
        ZonedDateTime beforeDST = ZonedDateTime.of(2021, 3, 28, 1, 0, 0, 0, ZoneId.of("Europe/Berlin"));
        ZonedDateTime afterDST = ZonedDateTime.of(2021, 3, 28, 3, 0, 0, 0, ZoneId.of("Europe/Berlin"));
        assertEquals(0, provider.diff(beforeDST, afterDST, "days"));
    }

    @Test
    void testDiffTimeZoneDifferences() {
        ZonedDateTime utc = ZonedDateTime.of(2021, 3, 28, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime berlin = ZonedDateTime.of(2021, 3, 28, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"));
        assertEquals(0, provider.diff(utc, berlin, "days"));
        ZonedDateTime utcLate = ZonedDateTime.of(2021, 3, 28, 23, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime berlinNextDay = ZonedDateTime.of(2021, 3, 29, 1, 0, 0, 0, ZoneId.of("Europe/Berlin"));
        assertEquals(1, provider.diff(utcLate, berlinNextDay, "days"));
    }

    @Test
    void testDiffLeapYear() {
        assertEquals(2, provider.diff("2020-02-28", "2020-03-01", "days"));
    }

    @Test
    void testDiffEdgeCases() {
        assertNull(provider.diff(null, "2021-01-01", "days"));
        assertNull(provider.diff("2021-01-01", null, "days"));
        assertNull(provider.diff("2021-01-01", "2021-01-02", null));
        assertNull(provider.diff("2021-01-01", "2021-01-02", "invalid"));
    }


}
