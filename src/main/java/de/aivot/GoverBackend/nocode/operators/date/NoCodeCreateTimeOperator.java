package de.aivot.GoverBackend.nocode.operators.date;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

import java.time.ZonedDateTime;

public class NoCodeCreateTimeOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "create-time";
    }

    @Override
    public String getLabel() {
        return "Erstelle Zeit";
    }

    @Override
    public String getAbstract() {
        return "Erstellt eine Zeit basierend auf den angegebenen Stunden und Minuten.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Erstelle Zeit“** erstellt eine Zeit basierend auf den angegebenen Stunden und Minuten. \s
                Er wird verwendet, um eine Zeit aus einzelnen Komponenten zu erstellen.
                
                # Anwendungsbeispiel:
                Angenommen, Sie haben die Werte für Stunden und Minuten: `14`, `30`. \s
                Mit dem Operator **„Erstelle Zeit“** können Sie eine Zeit erstellen:
                
                ```text
                Erstelle Zeit (14, 30)
                ```
                
                **Ergebnis:** \s
                Die erstellte Zeit ist: `YYYY-MM-DDT14:30:00Z`.
                
                # Wann verwenden Sie den Operator „Erstelle Zeit“?
                Verwenden Sie **„Erstelle Zeit“**, wenn Sie:
                - Eine Zeit aus einzelnen Komponenten (Stunden, Minuten) erstellen möchten. \s
                - Zeiten in einem bestimmten Format benötigen. \s
                - Eine Zeit für Berechnungen oder Vergleiche erstellen möchten.
                """;
    }

    @Override
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.Number,
                        "Stunde"
                ),
                new NoCodeParameter(
                        NoCodeDataType.Number,
                        "Minute"
                ),
        };
    }

    @Override
    public NoCodeDataType getReturnType() {
        return NoCodeDataType.Date;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
        int hour = castToNumber(args[0]).intValue();
        int minute = castToNumber(args[1]).intValue();

        var time = ZonedDateTime
                .now()
                .withHour(hour)
                .withMinute(minute);

        return new NoCodeResult(NoCodeDataType.Date, time);
    }
}