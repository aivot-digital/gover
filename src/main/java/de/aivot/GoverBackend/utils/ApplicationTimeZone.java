package de.aivot.GoverBackend.utils;

import jakarta.annotation.Nonnull;

import java.time.ZoneId;

public final class ApplicationTimeZone {
    public static final String DEFAULT_TIMEZONE = "Europe/Berlin";

    private static volatile ZoneId zoneId = resolveInitialZoneId();

    private ApplicationTimeZone() {
    }

    @Nonnull
    public static ZoneId getZoneId() {
        return zoneId;
    }

    @Nonnull
    public static String getZoneIdValue() {
        return getZoneId().getId();
    }

    public static void configure(@Nonnull ZoneId configuredZoneId) {
        zoneId = configuredZoneId;
    }

    @Nonnull
    private static ZoneId resolveInitialZoneId() {
        var configuredZoneId = System.getProperty("user.timezone");
        if (configuredZoneId != null && !configuredZoneId.isBlank()) {
            return ZoneId.of(configuredZoneId);
        }

        configuredZoneId = System.getenv("GOVER_TIMEZONE");
        if (configuredZoneId != null && !configuredZoneId.isBlank()) {
            return ZoneId.of(configuredZoneId);
        }

        configuredZoneId = System.getenv("TZ");
        if (configuredZoneId != null && !configuredZoneId.isBlank()) {
            return ZoneId.of(configuredZoneId);
        }

        return ZoneId.of(DEFAULT_TIMEZONE);
    }
}
