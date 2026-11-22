package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDateInput;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.*;
import jakarta.annotation.Nullable;

import java.util.Locale;

public class NoCodeSubtractFromDateOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "subtract-from-date";
    }

    @Override
    public String getLabel() {
        return "Vom Datum abziehen";
    }

    @Override
    public String getAbstract() {
        return "Zieht Tage, Wochen, Monate oder Jahre von einem Datum ab.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Mit „Vom Datum abziehen“ berechnen Sie ein früheres Datum, beispielsweise einen Stichtag vor einem Termin.

                # Welche Werte werden benötigt?

                - **Ausgangsdatum:** Das Datum, von dem aus Sie rechnen möchten. Sie können das Datum selbst eingeben, aus einer verfügbaren Wertquelle übernehmen oder mit einer Funktion ermitteln. Für die eigene Eingabe wählen Sie unter „Datumsgenauigkeit“ Tag, Monat und Jahr, Monat und Jahr oder nur das Jahr. Es gibt keine festen Auswahlwerte.
                - **Anzahl:** Geben Sie eine ganze Zahl ein, beispielsweise `5`. Es gibt keine festen Auswahlwerte. Sie können die Anzahl auch aus einer verfügbaren Wertquelle übernehmen oder mit einer Funktion ermitteln.
                - **Einheit:** Wählen Sie Tage, Wochen, Monate oder Jahre. Zusätzliche eigene Einträge sind in dieser Auswahl nicht möglich. Über „Andere Wertquelle verwenden …“ können Sie die Einheit auch übernehmen oder mit einer Funktion ermitteln.

                # Welches Ergebnis erhalten Sie?

                Sie erhalten das berechnete Datum. Eine positive Anzahl führt zu einem früheren Datum.

                Enthält das Ausgangsdatum nur Monat und Jahr oder nur ein Jahr, enthält auch das Ergebnis nur diese Angaben.

                # Beispiel im No-Code-Editor

                Sie möchten das Datum ermitteln, das fünf Tage vor dem 15.08.2026 liegt.

                1. Wählen Sie die Funktion „Vom Datum abziehen“.
                2. Wählen Sie für „Ausgangsdatum“ die Option „Wert selbst festlegen“ und geben Sie `15.08.2026` ein.
                3. Wählen Sie für „Anzahl“ die Option „Wert selbst festlegen“ und geben Sie `5` ein.
                4. Wählen Sie für „Einheit“ die Option „Tage“.

                Der fertige Ausdruck liest sich so:

                **15.08.2026 minus 5 Tage**

                Das Ergebnis ist **10.08.2026**.

                # Was ist zu beachten?

                - Tage sind Kalendertage. Wochenenden und Feiertage werden mitgezählt. Eine Woche entspricht sieben Tagen.
                - Bei der Anzahl `0` bleibt das Datum gleich. Eine negative Anzahl kehrt die Rechenrichtung um: Mit `-5` erhalten Sie ein späteres Datum.
                - Gibt es den Tag im Zielmonat nicht, wird der letzte Tag dieses Monats verwendet. Beispiel: 31.03.2026 minus 1 Monat ergibt 28.02.2026.
                - Dasselbe gilt für Schaltjahre. Beispiel: 29.02.2024 minus 1 Jahr ergibt 28.02.2023.
                - Durch die Anpassung an Monatsenden erhalten Sie beim anschließenden Zurückrechnen nicht immer das Ausgangsdatum.
                - Enthält das Datum nur Monat und Jahr, können Sie Monate oder Jahre verwenden. Enthält es nur ein Jahr, können Sie nur Jahre verwenden.
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
                        ).withSentence("das Datum").withDateInput(NoCodeDateInput.Calendar),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Anzahl",
                                "Die Anzahl der gewählten Einheiten. Geben Sie eine ganze Zahl ein."
                        ).withSentence("minus").withNumberConstraints(new NoCodeNumberConstraints(
                                true, (double) Integer.MIN_VALUE, (double) Integer.MAX_VALUE
                        )),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Einheit",
                                "Wählen Sie Tage, Wochen, Monate oder Jahre.",
                                new NoCodeParameterOption("Tage", NoCodeAddToDateOperator.DAYS_UNIT, "Tag"),
                                new NoCodeParameterOption("Wochen", NoCodeAddToDateOperator.WEEKS_UNIT, "Woche"),
                                new NoCodeParameterOption("Monate", NoCodeAddToDateOperator.MONTHS_UNIT, "Monat"),
                                new NoCodeParameterOption("Jahre", NoCodeAddToDateOperator.YEARS_UNIT, "Jahr")
                        ).withSentence("").withQuantityParameterIndex(1)
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 minus #1 #2";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var date = requireCalendarValue(args[0], "Prüfen Sie die Datumsangabe für „Ausgangsdatum“.");
        var amount = requireInteger(args[1], "Die Anzahl muss eine ganze Zahl sein.");
        var unit = castToString(args[2]).trim().toLowerCase(Locale.ROOT);

        // Widen before negation so Integer.MIN_VALUE remains representable.
        return new NoCodeResult(NoCodeAddToDateOperator.adjustDate(date, -(long) amount, unit));
    }
}
