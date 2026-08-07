package de.aivot.gover.backend.plugins.core.v1.operators.date;

import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.nocode.enums.NoCodeDataType;
import de.aivot.gover.backend.nocode.exceptions.NoCodeException;
import de.aivot.gover.backend.nocode.models.NoCodeOperator;
import de.aivot.gover.backend.nocode.models.NoCodeParameter;
import de.aivot.gover.backend.nocode.models.NoCodeResult;
import de.aivot.gover.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.time.LocalTime;

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
        return "Erstellt eine lokale Uhrzeit aus Stunde, Minute und optional Sekunde.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Erstelle Zeit“** erstellt eine lokale Uhrzeit aus Stunde, Minute und optional Sekunde. \s
                Er wird verwendet, um eine Zeit aus einzelnen Komponenten zu erstellen.
                
                # Anwendungsbeispiel:
                Angenommen, Sie haben die Werte für Stunden und Minuten: `14`, `30`. \s
                Mit dem Operator **„Erstelle Zeit“** können Sie eine Zeit erstellen:
                
                ```text
                Erstelle Zeit (14, 30)
                ```
                
                **Ergebnis:** \s
                Die erstellte Zeit ist: `14:30:00`.
                
                # Wann verwenden Sie den Operator „Erstelle Zeit“?
                Verwenden Sie **„Erstelle Zeit“**, wenn Sie:
                - Eine Zeit aus einzelnen Komponenten (Stunden, Minuten und optional Sekunden) erstellen möchten. \s
                - Zeiten in einem bestimmten Format benötigen. \s
                - Eine Zeit für Berechnungen oder Vergleiche erstellen möchten.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        // Empty expressions use the first signature, so new expressions expose seconds.
        // Stored two-operand expressions still resolve the second signature by operand count.
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
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Sekunde",
                                "Die Sekunde der Zeit (0-59)."
                        )
                ),
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

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        // Templates are defined per operator, not per signature. A shared template would
        // either hide seconds from new expressions or leak a placeholder into legacy ones.
        // The generic humanizer lists exactly the operands of the resolved signature.
        return null;
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        int hour = requireInteger(args[0], "Die Stunde muss eine ganze Zahl sein.");
        int minute = requireInteger(args[1], "Die Minute muss eine ganze Zahl sein.");
        int second = args.length >= 3
                ? requireInteger(args[2], "Die Sekunde muss eine ganze Zahl sein.")
                : 0;

        if (hour < 0 || hour > 23) {
            throw new NoCodeException("Ungültige Stunde: " + hour + ". Erwartet 0-23.");
        }

        if (minute < 0 || minute > 59) {
            throw new NoCodeException("Ungültige Minute: " + minute + ". Erwartet 0-59.");
        }

        if (second < 0 || second > 59) {
            throw new NoCodeException("Ungültige Sekunde: " + second + ". Erwartet 0-59.");
        }

        var time = LocalTime.of(hour, minute, second);

        return new NoCodeResult(time);
    }
}
