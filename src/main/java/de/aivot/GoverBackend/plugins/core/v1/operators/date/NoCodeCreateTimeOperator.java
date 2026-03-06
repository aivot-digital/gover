package de.aivot.GoverBackend.plugins.core.v1.operators.date;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;

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
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Time,
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Stunde",
                                "Die Stunde der Zeit (0-23)."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Minute",
                                "Die Minute der Zeit (0-59)."
                        )
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(ElementData data, Object... args) throws NoCodeException {
        int hour = castToNumber(args[0]).intValue();
        int minute = castToNumber(args[1]).intValue();

        if (hour < 0 || hour > 23) {
            throw new NoCodeException("Ungültige Stunde: " + hour + ". Erwartet 0-23.");
        }

        if (minute < 0 || minute > 59) {
            throw new NoCodeException("Ungültige Minute: " + minute + ". Erwartet 0-59.");
        }

        var time = ZonedDateTime
                .now()
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0);

        return new NoCodeResult(time);
    }
}
