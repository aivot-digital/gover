package de.aivot.gover.backend.utils;

import jakarta.annotation.Nonnull;

import java.time.ZoneId;

public final class ApplicationTimeZone {
    public static final String DEFAULT_TIMEZONE = "Europe/Berlin";

    // Static and non-Spring code paths cannot inject BusinessTime. This bridge is
    // configured once from GoverConfig during startup and remains visible across threads.
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
        var configuredZoneId = System.getProperty("gover.timezone");
        if (configuredZoneId != null && !configuredZoneId.isBlank()) {
            return ZoneId.of(configuredZoneId);
        }

        configuredZoneId = System.getenv("GOVER_TIMEZONE");
        if (configuredZoneId != null && !configuredZoneId.isBlank()) {
            return ZoneId.of(configuredZoneId);
        }

        // Keep the non-Spring fallback aligned with application.yml. The host/JVM
        // timezone is intentionally ignored because Gover's business timezone is
        // configured explicitly and must not depend on the runtime environment.
        return ZoneId.of(DEFAULT_TIMEZONE);
    }
}
