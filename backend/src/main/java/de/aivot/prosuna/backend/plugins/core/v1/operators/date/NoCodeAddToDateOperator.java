package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.*;
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
        return "Zum Datum hinzufügen";
    }

    @Override
    public String getAbstract() {
        return "Fügt einem Datum Tage, Wochen, Monate oder Jahre hinzu.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Mit „Zum Datum hinzufügen“ berechnen Sie ein späteres Datum, beispielsweise das Ende einer Frist.

                # Welche Werte werden benötigt?

                - **Ausgangsdatum:** Das Datum, von dem aus Sie rechnen möchten. Sie können ein vollständiges Datum selbst eingeben, beispielsweise `15.08.2026`, aus einer verfügbaren Wertquelle übernehmen oder mit einer Funktion ermitteln. Es gibt keine festen Auswahlwerte.
                - **Anzahl:** Geben Sie eine ganze Zahl ein, beispielsweise `5`. Es gibt keine festen Auswahlwerte. Sie können die Anzahl auch aus einer verfügbaren Wertquelle übernehmen oder mit einer Funktion ermitteln.
                - **Einheit:** Wählen Sie Tage, Wochen, Monate oder Jahre. Zusätzliche eigene Einträge sind in dieser Auswahl nicht möglich. Über „Andere Wertquelle verwenden …“ können Sie die Einheit auch übernehmen oder mit einer Funktion ermitteln.

                # Welches Ergebnis erhalten Sie?

                Sie erhalten das berechnete Datum. Eine positive Anzahl führt zu einem späteren Datum.

                Enthält das Ausgangsdatum nur Monat und Jahr oder nur ein Jahr, enthält auch das Ergebnis nur diese Angaben.

                # Beispiel im No-Code-Editor

                Sie möchten das Datum ermitteln, das fünf Tage nach dem 15.08.2026 liegt.

                1. Wählen Sie die Funktion „Zum Datum hinzufügen“.
                2. Wählen Sie für „Ausgangsdatum“ die Option „Wert selbst festlegen“ und geben Sie `15.08.2026` ein.
                3. Wählen Sie für „Anzahl“ die Option „Wert selbst festlegen“ und geben Sie `5` ein.
                4. Wählen Sie für „Einheit“ die Option „Tage“.

                Der fertige Ausdruck liest sich so:

                **15.08.2026 plus 5 Tage**

                Das Ergebnis ist **20.08.2026**.

                # Was ist zu beachten?

                - Tage sind Kalendertage. Wochenenden und Feiertage werden mitgezählt. Eine Woche entspricht sieben Tagen.
                - Bei der Anzahl `0` bleibt das Datum gleich. Eine negative Anzahl kehrt die Rechenrichtung um: Mit `-5` erhalten Sie ein früheres Datum.
                - Gibt es den Tag im Zielmonat nicht, wird der letzte Tag dieses Monats verwendet. Beispiel: 31.01.2026 plus 1 Monat ergibt 28.02.2026.
                - Dasselbe gilt für Schaltjahre. Beispiel: 29.02.2024 plus 1 Jahr ergibt 28.02.2025.
                - Durch die Anpassung an Monatsenden erhalten Sie beim anschließenden Zurückrechnen nicht immer das Ausgangsdatum.
                - Enthält ein übernommenes Datum nur Monat und Jahr, können Sie Monate oder Jahre verwenden. Enthält es nur ein Jahr, können Sie nur Jahre verwenden. Im Eingabefeld für ein selbst festgelegtes Ausgangsdatum können Sie derzeit nur vollständige Datumsangaben eingeben.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Date,
                        new NoCodeParameter(
                                NoCodeDataType.Date,
                                "Ausgangsdatum",
                                "Das Datum, von dem aus Sie rechnen möchten."
                        ).withSentence("das Datum"),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Anzahl",
                                "Die Anzahl der gewählten Einheiten. Geben Sie eine ganze Zahl ein."
                        ).withSentence("plus").withNumberConstraints(new NoCodeNumberConstraints(
                                true, (double) Integer.MIN_VALUE, (double) Integer.MAX_VALUE
                        )),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Einheit",
                                "Wählen Sie Tage, Wochen, Monate oder Jahre.",
                                new NoCodeParameterOption("Tage", DAYS_UNIT, "Tag"),
                                new NoCodeParameterOption("Wochen", WEEKS_UNIT, "Woche"),
                                new NoCodeParameterOption("Monate", MONTHS_UNIT, "Monat"),
                                new NoCodeParameterOption("Jahre", YEARS_UNIT, "Jahr")
                        ).withSentence("").withQuantityParameterIndex(1)
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 plus #1 #2";
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
                    default -> throw new NoCodeException("Bei einem Datum mit Monat und Jahr können Sie nur Monate oder Jahre verwenden.");
                };
                case Year year -> {
                    if (!YEARS_UNIT.equals(unit)) {
                        throw new NoCodeException("Bei einer Jahresangabe können Sie nur Jahre verwenden.");
                    }
                    yield year.plusYears(amount);
                }
                default -> throw new NoCodeException("Nicht unterstützter Datumswert.");
            };
        } catch (DateTimeException | ArithmeticException exception) {
            throw new NoCodeException("Der Datumswert liegt außerhalb des unterstützten Bereichs.");
        }
    }
}
