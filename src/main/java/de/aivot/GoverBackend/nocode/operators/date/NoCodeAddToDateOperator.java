package de.aivot.GoverBackend.nocode.operators.date;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeParameterOption;

public class NoCodeAddToDateOperator extends NoCodeOperator {
    public static final String DAYS_UNIT = "tage";
    public static final String WEEKS_UNIT = "wochen";
    public static final String MONTHS_UNIT = "monate";
    public static final String YEARS_UNIT = "jahre";

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
                        new NoCodeParameterOption("Tage", DAYS_UNIT),
                        new NoCodeParameterOption("Wochen", WEEKS_UNIT),
                        new NoCodeParameterOption("Monate", MONTHS_UNIT),
                        new NoCodeParameterOption("Jahre", YEARS_UNIT)
                ),
        };
    }

    @Override
    public NoCodeDataType getReturnType() {
        return NoCodeDataType.Date;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
        var date = castToDateTime(args[0]);
        var amount = castToNumber(args[1]).intValue();
        var unit = castToString(args[2]);

        date = switch (unit.toLowerCase()) {
            case DAYS_UNIT -> date.plusDays(amount);
            case WEEKS_UNIT -> date.plusWeeks(amount);
            case MONTHS_UNIT -> date.plusMonths(amount);
            case YEARS_UNIT -> date.plusYears(amount);
            default -> throw new NoCodeException("Ungültige Einheit: " + unit);
        };

        return new NoCodeResult(NoCodeDataType.Date, date);
    }
}