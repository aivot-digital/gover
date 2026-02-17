package de.aivot.GoverBackend.plugins.core.v1.operators.date;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.*;

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

    @Override
    public NoCodeResult performEvaluation(ElementData data, Object... args) throws NoCodeException {
        if (args.length != 3) {
            throw new NoCodeWrongArgumentCountException(3, args.length);
        }

        var date = castToDateTime(args[0]);
        var amount = castToNumber(args[1]).intValue();
        var unit = castToString(args[2]);

        date = switch (unit.toLowerCase()) {
            case NoCodeAddToDateOperator.DAYS_UNIT -> date.minusDays(amount);
            case NoCodeAddToDateOperator.WEEKS_UNIT -> date.minusWeeks(amount);
            case NoCodeAddToDateOperator.MONTHS_UNIT -> date.minusMonths(amount);
            case NoCodeAddToDateOperator.YEARS_UNIT -> date.minusYears(amount);
            default -> throw new NoCodeException("Ungültige Einheit: " + unit);
        };

        return new NoCodeResult(date);
    }
}