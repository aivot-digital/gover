package de.aivot.prosuna.backend.system.enums;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DashboardActivityPeriodTest {
    @Test
    void shouldResolveStableConfigValues() throws ResponseException {
        assertEquals(DashboardActivityPeriod.ThirtyDays, DashboardActivityPeriod.fromConfigValue("0"));
        assertEquals(DashboardActivityPeriod.ThreeMonths, DashboardActivityPeriod.fromConfigValue("1"));
        assertThrows(ResponseException.class, () -> DashboardActivityPeriod.fromConfigValue("2"));
    }

    @Test
    void thirtyDaysShouldUseDailyBucketsIncludingToday() {
        var today = LocalDate.of(2026, 8, 11);

        assertEquals(LocalDate.of(2026, 7, 13), DashboardActivityPeriod.ThirtyDays.getFirstBucketStart(today));
        assertEquals(today, DashboardActivityPeriod.ThirtyDays.getLastBucketStart(today));
        assertEquals(1, DashboardActivityPeriod.ThirtyDays.getBucketDays());
    }

    @Test
    void threeMonthsShouldUseThirteenMondayBuckets() {
        var today = LocalDate.of(2026, 8, 11);

        assertEquals(LocalDate.of(2026, 5, 18), DashboardActivityPeriod.ThreeMonths.getFirstBucketStart(today));
        assertEquals(LocalDate.of(2026, 8, 10), DashboardActivityPeriod.ThreeMonths.getLastBucketStart(today));
        assertEquals(7, DashboardActivityPeriod.ThreeMonths.getBucketDays());
    }
}
