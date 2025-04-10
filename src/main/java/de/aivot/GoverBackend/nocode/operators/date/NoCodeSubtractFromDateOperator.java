package de.aivot.GoverBackend.nocode.operators.date;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeParameterOption;

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
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.Date,
                        "Datum"
                ),
                new NoCodeParameter(
                        NoCodeDataType.Number,
                        "Anzahl"
                ),
                new NoCodeParameter(
                        NoCodeDataType.String,
                        "Einheit",
                        new NoCodeParameterOption("Tage", NoCodeAddToDateOperator.DAYS_UNIT),
                        new NoCodeParameterOption("Wochen", NoCodeAddToDateOperator.WEEKS_UNIT),
                        new NoCodeParameterOption("Monate", NoCodeAddToDateOperator.MONTHS_UNIT),
                        new NoCodeParameterOption("Jahre", NoCodeAddToDateOperator.YEARS_UNIT)
                ),
        };
    }

    @Override
    public NoCodeDataType getReturnType() {
        return NoCodeDataType.Date;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
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

        return new NoCodeResult(NoCodeDataType.Date, date);
    }
}