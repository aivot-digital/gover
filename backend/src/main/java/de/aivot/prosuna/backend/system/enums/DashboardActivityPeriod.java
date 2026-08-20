package de.aivot.prosuna.backend.system.enums;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;

/** Time ranges and aggregation intervals supported by the dashboard activity chart. */
public enum DashboardActivityPeriod {
    ThirtyDays(0, 30, 1),
    ThreeMonths(1, 13, 7),
    ;

    private final short configValue;
    private final int bucketCount;
    private final int bucketDays;

    DashboardActivityPeriod(int configValue, int bucketCount, int bucketDays) {
        if (configValue < Short.MIN_VALUE || configValue > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Dashboard activity period value must fit into a smallint");
        }
        this.configValue = (short) configValue;
        this.bucketCount = bucketCount;
        this.bucketDays = bucketDays;
    }

    @Nonnull
    public String getConfigValue() {
        return Short.toString(configValue);
    }

    public int getBucketDays() {
        return bucketDays;
    }

    @Nonnull
    public LocalDate getLastBucketStart(@Nonnull LocalDate today) {
        return bucketDays == 1
                ? today
                : today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    @Nonnull
    public LocalDate getFirstBucketStart(@Nonnull LocalDate today) {
        return getLastBucketStart(today).minusDays((long) (bucketCount - 1) * bucketDays);
    }

    @Nonnull
    public static DashboardActivityPeriod fromConfigValue(@Nonnull String value) throws ResponseException {
        return Arrays.stream(values())
                .filter(period -> period.getConfigValue().equals(value))
                .findFirst()
                .orElseThrow(() -> ResponseException.internalServerError(
                        "Ungültiger Zeitraum für die Vorgangsaktivität: " + value
                ));
    }
}
