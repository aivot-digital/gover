package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.time.LocalTime;

public class NoCodeCreateTimeOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "create-time";
    }

    @Override
    public String getLabel() {
        return "Uhrzeit bilden";
    }

    @Override
    public String getAbstract() {
        return "Bildet eine Uhrzeit aus Stunde, Minute und Sekunde.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Bilden Sie eine Uhrzeit, wenn Stunde, Minute und Sekunde einzeln vorliegen, zum Beispiel für die tägliche Öffnungszeit.

                # Welche Werte werden benötigt?
                - **Stunde:** Die Stunde im 24-Stunden-Format.
                - **Minute:** Die Minute innerhalb der Stunde.
                - **Sekunde:** Die Sekunde innerhalb der Minute.

                Sie können ganze Zahlen selbst eingeben, aus einer anderen Wertquelle übernehmen oder mit einer Funktion ermitteln. Vorgegebene Auswahlwerte gibt es nicht.

                # Welches Ergebnis erhalten Sie?
                Eine Uhrzeit auf die Sekunde genau, ohne Datum und ohne Zeitzone. Diese können Sie beispielsweise mit **„Uhrzeiten vergleichen“** prüfen.

                # Beispiel im No-Code-Editor
                1. Wählen Sie im Eingabefeld **„Uhrzeit“** von **„Uhrzeiten vergleichen“** die Funktion **„Uhrzeit bilden“**.
                2. Geben Sie als **„Stunde“** **14**, als **„Minute“** **30** und als **„Sekunde“** **0** ein.
                3. Vergleichen Sie das Ergebnis beispielsweise mit der Uhrzeit aus einem Formularelement.

                Das Ergebnis ist **14:30:00 Uhr**. Die zugeklappte Darstellung nennt alle eingegebenen Bestandteile: **„Uhrzeit bilden: Stunde: 14; Minute: 30; Sekunde: 0“**.

                # Was ist zu beachten?
                Die Funktion bildet eine Uhrzeit und keinen Zeitpunkt. Wenn Sie zusätzlich ein Datum berücksichtigen möchten, verwenden Sie **„Datum und Uhrzeit verbinden“**.

                Ältere Ausdrücke können nur Stunde und Minute enthalten. Dort wird die Sekunde als **0** ergänzt.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt die Uhrzeit";
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
                                "Die gewünschte Stunde im 24-Stunden-Format."
                        ).withSentence("aus Stunde"),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Minute",
                                "Die Minute innerhalb der angegebenen Stunde."
                        ).withSentence("Minute"),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Sekunde",
                                "Die Sekunde innerhalb der angegebenen Minute."
                        ).withSentence("und Sekunde")
                ),
                NoCodeSignatur.of(
                        NoCodeDataType.Time,
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Stunde",
                                "Die gewünschte Stunde im 24-Stunden-Format."
                        ).withSentence("aus Stunde"),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Minute",
                                "Die Minute innerhalb der angegebenen Stunde."
                        ).withSentence("Minute")
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
