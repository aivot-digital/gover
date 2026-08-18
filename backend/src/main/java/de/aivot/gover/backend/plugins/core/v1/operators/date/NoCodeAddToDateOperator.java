package de.aivot.gover.backend.plugins.core.v1.operators.date;

import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.nocode.enums.NoCodeDataType;
import de.aivot.gover.backend.nocode.exceptions.NoCodeException;
import de.aivot.gover.backend.nocode.models.*;
import jakarta.annotation.Nullable;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.Set;

public class NoCodeAddToDateOperator extends NoCodeOperator {
    public static final String DAYS_UNIT = "tage";
    public static final String WEEKS_UNIT = "wochen";
    public static final String MONTHS_UNIT = "monate";
    public static final String YEARS_UNIT = "jahre";
    private static final Set<String> SUPPORTED_UNITS = Set.of(
            DAYS_UNIT,
            WEEKS_UNIT,
            MONTHS_UNIT,
            YEARS_UNIT
    );

    @Override
    public String getIdentifier() {
        return "add-to-date";
    }

    @Override
    public String getLabel() {
        return "Zu Datum hinzufügen";
    }

    @Override
    public String getAbstract() {
        return "Fügt einem gegebenen Datum eine bestimmte Anzahl von Tagen, Wochen, Monaten oder Jahren hinzu.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Zu Datum hinzufügen“** fügt einem gegebenen Datum eine bestimmte Anzahl von Tagen, Wochen, Monaten oder Jahren hinzu. \s
                Er wird verwendet, um Datumsarithmetik durchzuführen.
                
                Verfügbare Einheiten:
                - Tage \s
                - Wochen \s
                - Monate \s
                - Jahre
                
                # Anwendungsbeispiel:
                Angenommen, Sie haben ein Datum `2023-08-15` und möchten 5 Tage hinzufügen. \s
                Mit dem Operator **„Zu Datum hinzufügen“** können Sie diese Operation durchführen:
                
                ```text
                Zu Datum hinzufügen ("2023-08-15", 5, "Tage")
                ```
                
                **Ergebnis:** \s
                Das resultierende Datum ist: `2023-08-20`.
                
                # Wann verwenden Sie den Operator „Zu Datum hinzufügen“?
                Verwenden Sie **„Zu Datum hinzufügen“**, wenn Sie:
                - Datumsarithmetik durchführen müssen. \s
                - Zukünftige oder vergangene Daten basierend auf einem gegebenen Datum berechnen möchten.

                Teil-Datumswerte behalten ihre Präzision. Monate können nur um Monate oder Jahre,
                reine Jahreswerte nur um Jahre verschoben werden.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Date,
                        new NoCodeParameter(
                                NoCodeDataType.Date,
                                "Datum",
                                "Das Ausgangsdatum, zu dem Tage, Wochen, Monate oder Jahre hinzugefügt werden sollen."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Anzahl",
                                "Die Anzahl der Tage, Wochen, Monate oder Jahre, die zum Datum hinzugefügt werden sollen."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Einheit",
                                "Die Einheit der hinzuzufügenden Zeitspanne (Tage, Wochen, Monate, Jahre).",
                                new NoCodeParameterOption("Tage", DAYS_UNIT),
                                new NoCodeParameterOption("Wochen", WEEKS_UNIT),
                                new NoCodeParameterOption("Monate", MONTHS_UNIT),
                                new NoCodeParameterOption("Jahre", YEARS_UNIT)
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "füge „#1“ „#2“ zu „#0“ hinzu";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var date = requireCalendarValue(args[0], "Ungültiger Datumswert: " + castToString(args[0]));
        var amount = requireInteger(args[1], "Die Anzahl muss eine ganze Zahl sein.");
        var unit = castToString(args[2]).trim().toLowerCase(Locale.ROOT);

        return new NoCodeResult(adjustDate(date, amount, unit));
    }

    static TemporalAccessor adjustDate(
            TemporalAccessor date,
            long amount,
            String unit
    ) throws NoCodeException {
        if (!SUPPORTED_UNITS.contains(unit)) {
            throw new NoCodeException("Ungültige Einheit: " + unit);
        }

        try {
            return switch (date) {
                case LocalDate localDate -> switch (unit) {
                    case DAYS_UNIT -> localDate.plusDays(amount);
                    case WEEKS_UNIT -> localDate.plusWeeks(amount);
                    // java.time intentionally resolves invalid target month-days, such
                    // as January 31 plus one month, to the last valid day of that month.
                    case MONTHS_UNIT -> localDate.plusMonths(amount);
                    case YEARS_UNIT -> localDate.plusYears(amount);
                    default -> throw new IllegalStateException("Validated unit is not handled");
                };
                case YearMonth yearMonth -> switch (unit) {
                    case MONTHS_UNIT -> yearMonth.plusMonths(amount);
                    case YEARS_UNIT -> yearMonth.plusYears(amount);
                    default -> throw incompatiblePrecision(unit, "Monat");
                };
                case Year year -> {
                    if (!YEARS_UNIT.equals(unit)) {
                        throw incompatiblePrecision(unit, "Jahr");
                    }
                    yield year.plusYears(amount);
                }
                default -> throw new NoCodeException("Nicht unterstützter Datumswert.");
            };
        } catch (DateTimeException | ArithmeticException exception) {
            throw new NoCodeException("Der Datumswert liegt außerhalb des unterstützten Bereichs.");
        }
    }

    private static NoCodeException incompatiblePrecision(String unit, String precision) {
        return new NoCodeException(
                "Die Einheit %s kann nicht auf einen Datumswert mit der Präzision %s angewendet werden."
                        .formatted(unit, precision)
        );
    }
}
