package de.aivot.GoverBackend.plugins.core.v1.javascript;

import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.GoverBackend.plugins.core.Core;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graalvm.polyglot.HostAccess;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;

@Service
public class DateJavascriptV1 implements JavascriptFunctionProvider {
    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
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
    public String getDescription() {
        return "Dieses Modul stellt Funktionen zur Verarbeitung von Datumswerten bereit.";
    }

    @Override
    public String[] getMethodTypeDefinitions() {
        return new String[]{
                "createDate(): Date;",
                "today(): string;",
                "now(): string;",
                "createDate(date: Date | string | number): Date | null;",
                "isSameDay(dateA: Date | string | number, dateB: Date | string | number): boolean;",
                "isBefore(dateA: Date | string | number, dateB: Date | string | number): boolean;",
                "isBeforeOrSameDay(dateA: Date | string | number, dateB: Date | string | number): boolean;",
                "isAfter(dateA: Date | string | number, dateB: Date | string | number): boolean;",
                "isAfterOrSameDay(dateA: Date | string | number, dateB: Date | string | number): boolean;",
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

    private static final DateTimeFormatter isoDateDateFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter germanDateFormatter = DateTimeFormatter
            .ofPattern("dd.MM.yyyy")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter[] availableDateFormatters = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_DATE_TIME,
            isoDateDateFormatter,
            germanDateFormatter
    };

    @HostAccess.Export
    public ZonedDateTime createDate() {
        return LocalDate
                .now()
                .atStartOfDay(ZoneId.systemDefault());
    }

    @HostAccess.Export
    public String today() {
        var date = createDate();
        return formatDate(date, "dd.MM.yyyy");
    }

    @HostAccess.Export
    public String now() {
        var date = createDate();
        return formatDate(date, "dd.MM.yyyy hh:mm") + " Uhr";
    }

    @Nullable
    @HostAccess.Export
    public ZonedDateTime createDate(@Nullable Object date) {
        if (date == null) {
            return null;
        }

        return switch (date) {
            case ZonedDateTime zonedDateTime -> zonedDateTime;
            case Number number -> {
                long epochMilli = number.longValue();
                yield ZonedDateTime.ofInstant(
                        Instant.ofEpochSecond(epochMilli),
                        ZoneId.systemDefault()
                );
            }
            case String dateString -> {
                for (DateTimeFormatter formatter : availableDateFormatters) {
                    try {
                        yield LocalDate
                                .parse(dateString, formatter)
                                .atStartOfDay(ZoneId.systemDefault());
                    } catch (Exception e) {
                        // Try next format
                    }
                }
                yield null;
            }
            default -> null;
        };
    }

    @HostAccess.Export
    public Boolean isSameDay(Object dateARaw, Object dateBRaw) {
        var dateA = createDate(dateARaw);
        var dateB = createDate(dateBRaw);

        if (dateA == null || dateB == null) {
            return false;
        }

        if (dateA.getYear() != dateB.getYear()) {
            return false;
        }

        if (dateA.getMonth() != dateB.getMonth()) {
            return false;
        }

        if (dateA.getDayOfMonth() != dateB.getDayOfMonth()) {
            return false;
        }

        return true;
    }

    @HostAccess.Export
    public boolean isBefore(Object dateARaw, Object dateBRaw) {
        var dateA = createDate(dateARaw);
        var dateB = createDate(dateBRaw);

        if (dateA == null || dateB == null) {
            return false;
        }

        if (dateA.getYear() > dateB.getYear()) {
            return false;
        }

        if (dateA.getYear() < dateB.getYear()) {
            return true;
        }

        if (dateA.getMonthValue() > dateB.getMonthValue()) {
            return false;
        }

        if (dateA.getMonthValue() < dateB.getMonthValue()) {
            return true;
        }

        if (dateA.getDayOfMonth() > dateB.getDayOfMonth()) {
            return false;
        }

        if (dateA.getDayOfMonth() < dateB.getDayOfMonth()) {
            return true;
        }

        return false;
    }

    @HostAccess.Export
    public boolean isBeforeOrSameDay(Object dateARaw, Object dateBRaw) {
        var dateA = createDate(dateARaw);
        var dateB = createDate(dateBRaw);

        return isBefore(dateA, dateB) || isSameDay(dateA, dateB);
    }

    @HostAccess.Export
    public boolean isAfter(Object dateARaw, Object dateBRaw) {
        var dateA = createDate(dateARaw);
        var dateB = createDate(dateBRaw);

        if (dateA == null || dateB == null) {
            return false;
        }

        if (dateA.getYear() < dateB.getYear()) {
            return false;
        }

        if (dateA.getYear() > dateB.getYear()) {
            return true;
        }

        if (dateA.getMonthValue() < dateB.getMonthValue()) {
            return false;
        }

        if (dateA.getMonthValue() > dateB.getMonthValue()) {
            return true;
        }

        if (dateA.getDayOfMonth() < dateB.getDayOfMonth()) {
            return false;
        }

        if (dateA.getDayOfMonth() > dateB.getDayOfMonth()) {
            return true;
        }

        return false;
    }

    @HostAccess.Export
    public boolean isAfterOrSameDay(Object dateARaw, Object dateBRaw) {
        var dateA = createDate(dateARaw);
        var dateB = createDate(dateBRaw);

        return isAfter(dateA, dateB) || isSameDay(dateA, dateB);
    }

    @HostAccess.Export
    public ZonedDateTime addDays(Object dateRaw, int days) {
        var date = createDate(dateRaw);

        if (date == null) {
            return null;
        }

        return date.plusDays(days);
    }

    @HostAccess.Export
    public ZonedDateTime addWeeks(Object dateRaw, int weeks) {
        var date = createDate(dateRaw);

        if (date == null) {
            return null;
        }

        return date.plusWeeks(weeks);
    }

    @HostAccess.Export
    public ZonedDateTime addMonths(Object dateRaw, int months) {
        var date = createDate(dateRaw);

        if (date == null) {
            return null;
        }

        return date.plusMonths(months);
    }

    @HostAccess.Export
    public ZonedDateTime addYears(Object dateRaw, int years) {
        var date = createDate(dateRaw);

        if (date == null) {
            return null;
        }

        return date.plusYears(years);
    }

    @HostAccess.Export
    public ZonedDateTime subtractDays(Object dateRaw, int days) {
        var date = createDate(dateRaw);

        if (date == null) {
            return null;
        }

        return date.minusDays(days);
    }

    @HostAccess.Export
    public ZonedDateTime subtractWeeks(Object dateRaw, int weeks) {
        var date = createDate(dateRaw);

        if (date == null) {
            return null;
        }

        return date.minusWeeks(weeks);
    }

    @HostAccess.Export
    public ZonedDateTime subtractMonths(Object dateRaw, int months) {
        var date = createDate(dateRaw);

        if (date == null) {
            return null;
        }

        return date.minusMonths(months);
    }

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
                    .withZone(ZoneId.systemDefault());
            return formatter.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    @HostAccess.Export
    public Number diff(Object startRaw, Object endRaw, String unit) {
        var start = createDate(startRaw);
        var end = createDate(endRaw);

        if (start == null || end == null || unit == null) {
            return null;
        }

        return switch (unit.toLowerCase()) {
            case "days" -> (int) Duration.between(start.toLocalDate().atStartOfDay(), end.toLocalDate().atStartOfDay()).toDays();
            case "weeks" -> Duration.between(start.toLocalDate().atStartOfDay(), end.toLocalDate().atStartOfDay()).toDays() / 7.f;
            case "months" -> (end.getYear() - start.getYear()) * 12 + (end.getMonthValue() - start.getMonthValue());
            case "years" -> end.getYear() - start.getYear();
            default -> null;
        };
    }

}
