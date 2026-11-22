package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeNumberConstraints;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameterOption;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.DateTimeException;
import java.util.Locale;

import static de.aivot.prosuna.backend.plugins.core.v1.operators.date.NoCodeAddToDateOperator.DAYS_UNIT;
import static de.aivot.prosuna.backend.plugins.core.v1.operators.date.NoCodeAddToDateOperator.MONTHS_UNIT;
import static de.aivot.prosuna.backend.plugins.core.v1.operators.date.NoCodeAddToDateOperator.WEEKS_UNIT;
import static de.aivot.prosuna.backend.plugins.core.v1.operators.date.NoCodeAddToDateOperator.YEARS_UNIT;

/**
 * Date-time counterpart of add-to-date and subtract-from-date. Calendar units are applied in the application time
 * zone, so "1 day" normally keeps the local wall-clock time across daylight-saving changes. A target time in a
 * daylight-saving gap advances by the gap duration, following java.time's calendar arithmetic.
 */
public class NoCodeAdjustDateTimeOperator extends NoCodeOperator {
    public enum Direction {
        ADD,
        SUBTRACT,
    }

    private final Direction direction;

    public NoCodeAdjustDateTimeOperator(Direction direction) {
        this.direction = direction;
    }

    @Override
    public String getIdentifier() {
        return switch (direction) {
            case ADD -> "add-to-datetime";
            case SUBTRACT -> "subtract-from-datetime";
        };
    }

    @Override
    public String getLabel() {
        return switch (direction) {
            case ADD -> "Zum Zeitpunkt hinzufügen";
            case SUBTRACT -> "Vom Zeitpunkt abziehen";
        };
    }

    @Override
    public String getAbstract() {
        return switch (direction) {
            case ADD -> "Fügt einem Zeitpunkt Tage, Wochen, Monate oder Jahre hinzu.";
            case SUBTRACT -> "Zieht Tage, Wochen, Monate oder Jahre von einem Zeitpunkt ab.";
        };
    }

    @Override
    public String getDescription() {
        var isAddition = direction == Direction.ADD;
        var resultDirection = isAddition ? "späteren" : "früheren";
        var relativePosition = isAddition ? "nach" : "vor";
        var operation = isAddition ? "plus" : "minus";
        var exampleResult = isAddition ? "27.11.2026" : "17.11.2026";
        var daylightSavingExample = isAddition
                ? "28.03.2026 um 02:30 Uhr plus 1 Tag ergibt 29.03.2026 um **03:30 Uhr**."
                : "30.03.2026 um 02:30 Uhr minus 1 Tag ergibt 29.03.2026 um **03:30 Uhr**.";
        return """
                # Wofür verwenden Sie diese Funktion?

                Mit „%s“ berechnen Sie einen %s Zeitpunkt, beispielsweise einen Termin fünf Tage %s einem Ausgangszeitpunkt.

                # Welche Werte werden benötigt?

                - **Ausgangszeitpunkt:** Das Datum mit Uhrzeit, von dem aus Sie rechnen möchten. Sie können den Zeitpunkt selbst eingeben, aus einer verfügbaren Wertquelle übernehmen oder mit einer Funktion ermitteln. Es gibt keine festen Auswahlwerte.
                - **Anzahl:** Geben Sie eine ganze Zahl ein, beispielsweise `5`. Es gibt keine festen Auswahlwerte. Sie können die Anzahl auch aus einer verfügbaren Wertquelle übernehmen oder mit einer Funktion ermitteln.
                - **Einheit:** Wählen Sie Tage, Wochen, Monate oder Jahre. Zusätzliche eigene Einträge sind in dieser Auswahl nicht möglich. Über „Andere Wertquelle verwenden …“ können Sie die Einheit auch übernehmen oder mit einer Funktion ermitteln.

                # Welches Ergebnis erhalten Sie?

                Sie erhalten den berechneten Zeitpunkt mit Datum und Uhrzeit. Eine positive Anzahl führt zu einem %s Zeitpunkt.

                # Beispiel im No-Code-Editor

                Sie möchten einen Termin fünf Tage %s dem 22.11.2026 um 13:30 Uhr berechnen.

                1. Wählen Sie die Funktion „%s“.
                2. Wählen Sie für „Ausgangszeitpunkt“ die Option „Wert selbst festlegen“ und geben Sie `22.11.2026 13:30:00 Uhr` ein.
                3. Wählen Sie für „Anzahl“ die Option „Wert selbst festlegen“ und geben Sie `5` ein.
                4. Wählen Sie für „Einheit“ die Option „Tage“.

                Der fertige Ausdruck liest sich so:

                **22.11.2026 13:30:00 Uhr %s 5 Tage**

                Das Ergebnis ist **%s um 13:30 Uhr**.

                # Was ist zu beachten?

                - Gerechnet wird in der für Prosuna eingestellten Zeitzone.
                - Tage sind Kalendertage. Wochenenden und Feiertage werden mitgezählt. Eine Woche entspricht sieben Tagen.
                - Bei `0` bleibt der Ausgangszeitpunkt gleich. Eine negative Anzahl kehrt die Rechenrichtung um.
                - Gibt es den Tag im Zielmonat nicht, wird der letzte Tag dieses Monats verwendet. Das gilt auch für den 29. Februar beim Rechnen mit Jahren.
                - Die Uhrzeit bleibt nach Möglichkeit gleich. Wird die berechnete Zieluhrzeit bei einer Zeitumstellung übersprungen, wird sie um die übersprungene Zeit nach vorn verschoben. Beispiel für die Zeitzone Berlin: %s
                - Ein Kalendertag entspricht bei einer Zeitumstellung beispielsweise 23 oder 25 tatsächlich vergangenen Stunden.
                - Durch solche Anpassungen erhalten Sie beim anschließenden Zurückrechnen nicht immer den Ausgangszeitpunkt.
                """.formatted(
                getLabel(), resultDirection, relativePosition, resultDirection, relativePosition,
                getLabel(), operation, exampleResult, daylightSavingExample
        );
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.DateTime,
                        new NoCodeParameter(
                                NoCodeDataType.DateTime,
                                "Ausgangszeitpunkt",
                                "Das Datum mit Uhrzeit, von dem aus Sie rechnen möchten."
                        ).withSentence("den Zeitpunkt"),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Anzahl",
                                "Die Anzahl der gewählten Einheiten. Geben Sie eine ganze Zahl ein."
                        ).withSentence(direction == Direction.ADD ? "plus" : "minus").withNumberConstraints(new NoCodeNumberConstraints(
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
        return switch (direction) {
            case ADD -> "#0 plus #1 #2";
            case SUBTRACT -> "#0 minus #1 #2";
        };
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var dateTime = requireDateTime(args[0], "Der Ausgangszeitpunkt ist ungültig. Verwenden Sie einen Wert mit Datum und Uhrzeit.");
        var amount = requireInteger(args[1], "Die Anzahl muss eine ganze Zahl sein.");
        var unit = castToString(args[2]).trim().toLowerCase(Locale.ROOT);
        // Widen before negation so Integer.MIN_VALUE remains representable.
        var signedAmount = direction == Direction.ADD ? (long) amount : -(long) amount;

        try {
            var adjusted = switch (unit) {
                case DAYS_UNIT -> dateTime.plusDays(signedAmount);
                case WEEKS_UNIT -> dateTime.plusWeeks(signedAmount);
                case MONTHS_UNIT -> dateTime.plusMonths(signedAmount);
                case YEARS_UNIT -> dateTime.plusYears(signedAmount);
                default -> throw new NoCodeException("Die Einheit ist ungültig. Verwenden Sie Tage, Wochen, Monate oder Jahre.");
            };
            return new NoCodeResult(adjusted.toInstant());
        } catch (DateTimeException | ArithmeticException exception) {
            throw new NoCodeException("Der berechnete Zeitpunkt liegt außerhalb des unterstützten Bereichs. Prüfen Sie Ausgangszeitpunkt und Anzahl.");
        }
    }

    @Nonnull
    @Override
    public String getSentenceLead() {
        return "ergibt";
    }
}
