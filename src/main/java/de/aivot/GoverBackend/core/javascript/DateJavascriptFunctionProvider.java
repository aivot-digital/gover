package de.aivot.GoverBackend.core.javascript;

import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import org.graalvm.polyglot.HostAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class DateJavascriptFunctionProvider implements JavascriptFunctionProvider {
    private final Logger logger = LoggerFactory.getLogger(DateJavascriptFunctionProvider.class);

    @Override
    public String getPackageName() {
        return "date";
    }

    @Override
    public String getLabel() {
        return "Date";
    }

    @Override
    public String getDescription() {
        return "Dieses Paket enthält Funktionen für Datumsoperationen.";
    }

    @HostAccess.Export
    public ZonedDateTime createDate() {
        return ZonedDateTime.now();
    }

    private static final DateTimeFormatter isoDateFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter germanDateFormatter = DateTimeFormatter
            .ofPattern("dd.MM.yyyy")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter[] availableDateFormatters = new DateTimeFormatter[]{
            isoDateFormatter,
            germanDateFormatter
    };

    @HostAccess.Export
    public ZonedDateTime createDate(String date) {
        if (date == null || date.isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : availableDateFormatters) {
            try {
                return LocalDate
                        .parse(date, formatter)
                        .atStartOfDay(ZoneId.systemDefault());
            } catch (Exception e) {
                // Ignore and try the next formatter
                logger.error(e.getMessage(), e);
            }
        }

        return null;
    }

    @HostAccess.Export
    public Boolean isSameDay(ZonedDateTime dateA, ZonedDateTime dateB) {
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
    public boolean isBefore(ZonedDateTime dateA, ZonedDateTime dateB) {
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
    public boolean isBeforeOrSameDay(ZonedDateTime dateA, ZonedDateTime dateB) {
        return isBefore(dateA, dateB) || isSameDay(dateA, dateB);
    }

    @HostAccess.Export
    public boolean isAfter(ZonedDateTime dateA, ZonedDateTime dateB) {
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
    public boolean isAfterOrSameDay(ZonedDateTime dateA, ZonedDateTime dateB) {
        return isAfter(dateA, dateB) || isSameDay(dateA, dateB);
    }

    @HostAccess.Export
    public ZonedDateTime addDays(ZonedDateTime date, int days) {
        if (date == null) {
            return null;
        }
        return date.plusDays(days);
    }

    @HostAccess.Export
    public ZonedDateTime addWeeks(ZonedDateTime date, int weeks) {
        if (date == null) {
            return null;
        }
        return date.plusWeeks(weeks);
    }

    @HostAccess.Export
    public ZonedDateTime addMonths(ZonedDateTime date, int months) {
        if (date == null) {
            return null;
        }
        return date.plusMonths(months);
    }

    @HostAccess.Export
    public ZonedDateTime addYears(ZonedDateTime date, int years) {
        if (date == null) {
            return null;
        }
        return date.plusYears(years);
    }

    @HostAccess.Export
    public ZonedDateTime subtractDays(ZonedDateTime date, int days) {
        if (date == null) {
            return null;
        }
        return date.minusDays(days);
    }

    @HostAccess.Export
    public ZonedDateTime subtractWeeks(ZonedDateTime date, int weeks) {
        if (date == null) {
            return null;
        }
        return date.minusWeeks(weeks);
    }

    @HostAccess.Export
    public ZonedDateTime subtractMonths(ZonedDateTime date, int months) {
        if (date == null) {
            return null;
        }
        return date.minusMonths(months);
    }

    @HostAccess.Export
    public ZonedDateTime subtractYears(ZonedDateTime date, int years) {
        if (date == null) {
            return null;
        }
        return date.minusYears(years);
    }
}
