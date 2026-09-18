package de.aivot.prosuna.backend.plugins.core.v1.javascript;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import de.aivot.prosuna.backend.utils.IsoTimestampUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class DateJavascriptV1 implements JavascriptFunctionProvider {
    private static final DateTimeFormatter LOCAL_TIME_SECONDS_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final BusinessTime businessTime;

    // JavascriptEngine is also used directly outside Spring, most notably in isolated
    // script executions and tests. Production construction uses the injected BusinessTime.
    public DateJavascriptV1() {
        this(new BusinessTime(ApplicationTimeZone.getZoneId(), Clock.systemUTC()));
    }

    @Autowired
    public DateJavascriptV1(BusinessTime businessTime) {
        this.businessTime = businessTime;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "date";
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getName() {
        return "Datumsfunktionen";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Dieses Modul stellt Funktionen zur Verarbeitung von Datumswerten bereit.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Stellt JavaScript-Hilfsfunktionen für Datums-, Zeit- und Zeitzonenwerte bereit.

                Datumswerte können erzeugt, normalisiert, formatiert und in die lokale Anwendungszeitzone überführt werden. Darüber hinaus unterstützt das Modul Berechnungen wie das Addieren oder Subtrahieren von Tagen, Wochen, Monaten und Jahren sowie die Ermittlung von Zeitabständen.
                """;
    }

    @Override
    public String[] getMethodTypeDefinitions() {
        return new String[]{
                "createDate(): Date;",
                "today(): string;",
                "now(): string;",
                "todayIso(): string;",
                "nowIso(): string;",
                "getApplicationTimeZone(): string;",
                "resolveDateTime(date: string, time: string): string | null;",
                "toLocalDateIso(value: Date | string | number): string | null;",
                "toLocalTimeIso(value: Date | string | number): string | null;",
                "createDate(date: Date | string | number): Date | null;",
                "isSameDay(dateA: Date | string | number, dateB: Date | string | number): boolean;",
                "isBefore(dateA: Date | string | number, dateB: Date | string | number): boolean;",
                "isBeforeOrSameDay(dateA: Date | string | number, dateB: Date | string | number): boolean;",
                "isAfter(dateA: Date | string | number, dateB: Date | string | number): boolean;",
                "isAfterOrSameDay(dateA: Date | string | number, dateB: Date | string | number): boolean;",
                "isInstantBefore(dateA: Date | string | number, dateB: Date | string | number): boolean;",
                "isInstantAfter(dateA: Date | string | number, dateB: Date | string | number): boolean;",
                "addDays(date: Date | string | number, days: number): Date | null;",
                "addWeeks(date: Date | string | number, weeks: number): Date | null;",
                "addMonths(date: Date | string | number, months: number): Date | null;",
                "addYears(date: Date | string | number, years: number): Date | null;",
                "subtractDays(date: Date | string | number, days: number): Date | null;",
                "subtractWeeks(date: Date | string | number, weeks: number): Date | null;",
                "subtractMonths(date: Date | string | number, months: number): Date | null;",
                "subtractYears(date: Date | string | number, years: number): Date | null;",
                "formatDate(date: Date | string | number, format: string): string | null;",
                "diff(start: Date | string | number, end: Date | string | number, unit: 'days' | 'weeks' | 'months' | 'years'): number | null;"
        };
    }

    @HostAccess.Export
    public ZonedDateTime createDate() {
        return businessTime.today().atStartOfDay(businessTime.zoneId());
    }

    @HostAccess.Export
    public String today() {
        var date = createDate();
        return formatDate(date, "dd.MM.yyyy");
    }

    @HostAccess.Export
    public String now() {
        return formatDate(businessTime.zonedNow(), "dd.MM.yyyy HH:mm") + " Uhr";
    }

    @HostAccess.Export
    public String todayIso() {
        return businessTime.today().toString();
    }

    @HostAccess.Export
    public String nowIso() {
        return IsoTimestampUtils.toOffsetString(businessTime.now(), businessTime.zoneId());
    }

    @HostAccess.Export
    public String getApplicationTimeZone() {
        return businessTime.zoneId().getId();
    }

    @Nullable
    @HostAccess.Export
    public String resolveDateTime(@Nullable String date, @Nullable String time) {
        if (date == null || time == null) {
            return null;
        }

        try {
            var localDate = LocalDate.parse(date);
            var localTime = parseLocalTime(time);
            if (localTime == null) {
                return null;
            }

            var instant = businessTime.resolve(LocalDateTime.of(localDate, localTime));
            return IsoTimestampUtils.toOffsetString(instant, businessTime.zoneId());
        } catch (DateTimeException ignored) {
            // This includes malformed values and local date-times inside a DST gap.
            return null;
        }
    }

    @Nullable
    @HostAccess.Export
    public String toLocalDateIso(@Nullable Object value) {
        var dateTime = createDate(value);
        return dateTime == null ? null : dateTime.toLocalDate().toString();
    }

    @Nullable
    @HostAccess.Export
    public String toLocalTimeIso(@Nullable Object value) {
        var dateTime = createDate(value);
        return dateTime == null
                ? null
                : dateTime.toLocalTime().withNano(0).format(LOCAL_TIME_SECONDS_FORMATTER);
    }

    @Nullable
    @HostAccess.Export
    public ZonedDateTime createDate(@Nullable Object date) {
        if (date == null) {
            return null;
        }

        return switch (date) {
            case ZonedDateTime zonedDateTime -> zonedDateTime.withZoneSameInstant(businessTime.zoneId());
            case OffsetDateTime offsetDateTime -> offsetDateTime.toInstant().atZone(businessTime.zoneId());
            case Instant instant -> instant.atZone(businessTime.zoneId());
            case Date legacyDate -> legacyDate.toInstant().atZone(businessTime.zoneId());
            case LocalDate localDate -> localDate.atStartOfDay(businessTime.zoneId());
            case YearMonth yearMonth -> yearMonth.atDay(1).atStartOfDay(businessTime.zoneId());
            case Year year -> year.atDay(1).atStartOfDay(businessTime.zoneId());
            case LocalDateTime localDateTime -> resolveLocalDateTime(localDateTime);
            case Number number -> Instant.ofEpochMilli(number.longValue()).atZone(businessTime.zoneId());
            case Value guestValue -> parseGuestValue(guestValue);
            case String dateString -> parseDateString(dateString);
            default -> null;
        };
    }

    @Nullable
    @HostAccess.Export
    public ZonedDateTime createDate(@Nullable Value date) {
        // GraalJS resolves guest arguments to Value before ordinary Java conversions.
        // In particular, a JavaScript Date does not reliably arrive as java.util.Date,
        // so this overload must remain alongside createDate(Object).
        if (date == null || date.isNull()) {
            return null;
        }
        return parseGuestValue(date);
    }

    @Nullable
    private ZonedDateTime parseGuestValue(@Nonnull Value value) {
        try {
            if (value.isString()) {
                return parseDateString(value.asString());
            }
            if (value.isNumber()) {
                // JavaScript numbers are represented as doubles by GraalJS. Epoch
                // milliseconds are integral within JavaScript's safe integer range.
                return parseGuestEpochMillis(value);
            }
            if (value.canInvokeMember("getTime")) {
                // A JavaScript Date is a guest object. Calling getTime() through the
                // Polyglot API is the stable way to obtain its absolute epoch value.
                var epochMillis = value.invokeMember("getTime");
                if (epochMillis.isNumber()) {
                    return parseGuestEpochMillis(epochMillis);
                }
            }
        } catch (RuntimeException ignored) {
            // Guest member access and type conversion can throw PolyglotException or
            // ClassCastException. Invalid script input follows the provider's null contract.
            return null;
        }
        return null;
    }

    @Nullable
    private ZonedDateTime parseGuestEpochMillis(@Nonnull Value value) {
        var epochMillis = value.asDouble();
        // Invalid JavaScript Date instances return NaN from getTime(). Casting NaN to
        // long would otherwise turn invalid input into 1970-01-01T00:00:00Z.
        if (!Double.isFinite(epochMillis)) {
            return null;
        }
        return Instant.ofEpochMilli((long) epochMillis).atZone(businessTime.zoneId());
    }

    @Nullable
    private ZonedDateTime parseDateString(@Nonnull String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
                    .atStartOfDay(businessTime.zoneId());
        } catch (DateTimeParseException ignored) {
            // JavaScript exposes only one Date type. Partial calendar values therefore
            // use their first representable day when entering this compatibility API.
            try {
                return YearMonth.parse(value).atDay(1).atStartOfDay(businessTime.zoneId());
            } catch (DateTimeParseException ignoredMonth) {
                try {
                    return Year.parse(value).atDay(1).atStartOfDay(businessTime.zoneId());
                } catch (DateTimeParseException ignoredYear) {
                    try {
                        return IsoTimestampUtils.parseIsoInstant(value).atZone(businessTime.zoneId());
                    } catch (DateTimeParseException ignoredInstant) {
                        return null;
                    }
                }
            }
        }
    }

    @Nullable
    private LocalTime parseLocalTime(@Nonnull String value) {
        if (!value.matches("^\\d{2}:\\d{2}(?::\\d{2})?$")) {
            return null;
        }

        try {
            return LocalTime.parse(value).withNano(0);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    @Nullable
    private ZonedDateTime resolveLocalDateTime(@Nonnull LocalDateTime value) {
        try {
            return businessTime.resolve(value).atZone(businessTime.zoneId());
        } catch (DateTimeException ignored) {
            return null;
        }
    }

    @HostAccess.Export
    public boolean isSameDay(Object dateARaw, Object dateBRaw) {
        var dateA = createDate(dateARaw);
        var dateB = createDate(dateBRaw);

        if (dateA == null || dateB == null) {
            return false;
        }

        return dateA.toLocalDate().equals(dateB.toLocalDate());
    }

    @HostAccess.Export
    public boolean isBefore(Object dateARaw, Object dateBRaw) {
        var dateA = createDate(dateARaw);
        var dateB = createDate(dateBRaw);

        if (dateA == null || dateB == null) {
            return false;
        }

        return dateA.toLocalDate().isBefore(dateB.toLocalDate());
    }

    @HostAccess.Export
    public boolean isBeforeOrSameDay(Object dateARaw, Object dateBRaw) {
        var dateA = createDate(dateARaw);
        var dateB = createDate(dateBRaw);

        return dateA != null &&
                dateB != null &&
                !dateA.toLocalDate().isAfter(dateB.toLocalDate());
    }

    @HostAccess.Export
    public boolean isAfter(Object dateARaw, Object dateBRaw) {
        var dateA = createDate(dateARaw);
        var dateB = createDate(dateBRaw);

        if (dateA == null || dateB == null) {
            return false;
        }

        return dateA.toLocalDate().isAfter(dateB.toLocalDate());
    }

    @HostAccess.Export
    public boolean isAfterOrSameDay(Object dateARaw, Object dateBRaw) {
        var dateA = createDate(dateARaw);
        var dateB = createDate(dateBRaw);

        return dateA != null &&
                dateB != null &&
                !dateA.toLocalDate().isBefore(dateB.toLocalDate());
    }

    @HostAccess.Export
    public boolean isInstantBefore(Object dateARaw, Object dateBRaw) {
        var dateA = createDate(dateARaw);
        var dateB = createDate(dateBRaw);

        return dateA != null &&
                dateB != null &&
                dateA.toInstant().isBefore(dateB.toInstant());
    }

    @HostAccess.Export
    public boolean isInstantAfter(Object dateARaw, Object dateBRaw) {
        var dateA = createDate(dateARaw);
        var dateB = createDate(dateBRaw);

        return dateA != null &&
                dateB != null &&
                dateA.toInstant().isAfter(dateB.toInstant());
    }

    @Nullable
    @HostAccess.Export
    public ZonedDateTime addDays(Object dateRaw, int days) {
        var date = createDate(dateRaw);

        if (date == null) {
            return null;
        }

        return date.plusDays(days);
    }

    @Nullable
    @HostAccess.Export
    public ZonedDateTime addWeeks(Object dateRaw, int weeks) {
        var date = createDate(dateRaw);

        if (date == null) {
            return null;
        }

        return date.plusWeeks(weeks);
    }

    @Nullable
    @HostAccess.Export
    public ZonedDateTime addMonths(Object dateRaw, int months) {
        var date = createDate(dateRaw);

        if (date == null) {
            return null;
        }

        return date.plusMonths(months);
    }

    @Nullable
    @HostAccess.Export
    public ZonedDateTime addYears(Object dateRaw, int years) {
        var date = createDate(dateRaw);

        if (date == null) {
            return null;
        }

        return date.plusYears(years);
    }

    @Nullable
    @HostAccess.Export
    public ZonedDateTime subtractDays(Object dateRaw, int days) {
        var date = createDate(dateRaw);

        if (date == null) {
            return null;
        }

        return date.minusDays(days);
    }

    @Nullable
    @HostAccess.Export
    public ZonedDateTime subtractWeeks(Object dateRaw, int weeks) {
        var date = createDate(dateRaw);

        if (date == null) {
            return null;
        }

        return date.minusWeeks(weeks);
    }

    @Nullable
    @HostAccess.Export
    public ZonedDateTime subtractMonths(Object dateRaw, int months) {
        var date = createDate(dateRaw);

        if (date == null) {
            return null;
        }

        return date.minusMonths(months);
    }

    @Nullable
    @HostAccess.Export
    public ZonedDateTime subtractYears(Object dateRaw, int years) {
        var date = createDate(dateRaw);

        if (date == null) {
            return null;
        }

        return date.minusYears(years);
    }

    @Nullable
    @HostAccess.Export
    public String formatDate(Object dateRaw, String format) {
        var date = createDate(dateRaw);

        if (date == null || format == null || format.isBlank()) {
            return null;
        }

        try {
            var formatter = DateTimeFormatter
                    .ofPattern(format)
                    .withZone(businessTime.zoneId());
            return formatter.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    @HostAccess.Export
    public Number diff(Object startRaw, Object endRaw, String unit) {
        var start = createDate(startRaw);
        var end = createDate(endRaw);

        if (start == null || end == null || unit == null) {
            return null;
        }

        var calendarDays = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
        return switch (unit.toLowerCase()) {
            case "days" -> (int) calendarDays;
            case "weeks" -> calendarDays / 7.f;
            case "months" -> (end.getYear() - start.getYear()) * 12 + (end.getMonthValue() - start.getMonthValue());
            case "years" -> end.getYear() - start.getYear();
            default -> null;
        };
    }

}
