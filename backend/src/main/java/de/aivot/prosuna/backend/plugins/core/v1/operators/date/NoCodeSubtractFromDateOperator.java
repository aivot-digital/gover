package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
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
        return "Von Datum subtrahieren";
    }

    @Override
    public String getAbstract() {
        return "Subtrahiert eine bestimmte Anzahl von Tagen, Wochen, Monaten oder Jahren von einem gegebenen Datum.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Von Datum subtrahieren“** subtrahiert eine bestimmte Anzahl von Tagen, Wochen, Monaten oder Jahren von einem gegebenen Datum. \s
                Er wird verwendet, um Datumsarithmetik durchzuführen.
                
                Verfügbare Einheiten:
                - Tage \s
                - Wochen \s
                - Monate \s
                - Jahre
                
                # Anwendungsbeispiel:
                Angenommen, Sie haben ein Datum `2023-08-15` und möchten 5 Tage subtrahieren. \s
                Mit dem Operator **„Von Datum subtrahieren“** können Sie diese Operation durchführen:
                
                ```text
                Von Datum subtrahieren ("2023-08-15", 5, "Tage")
                ```
                
                **Ergebnis:** \s
                Das resultierende Datum ist: `2023-08-10`.
                
                # Wann verwenden Sie den Operator „Von Datum subtrahieren“?
                Verwenden Sie **„Von Datum subtrahieren“**, wenn Sie:
                - Datumsarithmetik durchführen müssen. \s
                - Vergangene Daten basierend auf einem gegebenen Datum berechnen möchten.

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
                                "Das Ausgangsdatum, von dem subtrahiert werden soll."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Anzahl",
                                "Die Anzahl der Einheiten, die vom Datum subtrahiert werden sollen."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Einheit",
                                "Die Einheit der Zeit, die subtrahiert werden soll (Tage, Wochen, Monate, Jahre).",
                                new NoCodeParameterOption("Tage", NoCodeAddToDateOperator.DAYS_UNIT),
                                new NoCodeParameterOption("Wochen", NoCodeAddToDateOperator.WEEKS_UNIT),
                                new NoCodeParameterOption("Monate", NoCodeAddToDateOperator.MONTHS_UNIT),
                                new NoCodeParameterOption("Jahre", NoCodeAddToDateOperator.YEARS_UNIT)
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "subtrahiere „#1“ „#2“ von „#0“";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var date = requireCalendarValue(args[0], "Ungültiger Datumswert: " + castToString(args[0]));
        var amount = requireInteger(args[1], "Die Anzahl muss eine ganze Zahl sein.");
        var unit = castToString(args[2]).trim().toLowerCase(Locale.ROOT);

        // Widen before negation so Integer.MIN_VALUE remains representable.
        return new NoCodeResult(NoCodeAddToDateOperator.adjustDate(date, -(long) amount, unit));
    }
}
